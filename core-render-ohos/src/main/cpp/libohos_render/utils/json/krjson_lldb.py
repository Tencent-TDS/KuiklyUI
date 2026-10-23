# Tencent is pleased to support the open source community by making KuiklyUI
# available.
# Copyright (C) 2026 Tencent. All rights reserved.
# Licensed under the License of KuiklyUI;
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# https://github.com/Tencent-TDS/KuiklyUI/blob/main/LICENSE
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""LLDB formatter for KRJSON (typedef uint64_t).

Layout matches Box.h. Summaries only read process memory. `krjson dump`
is the one path that calls KRJSONDump / KRJSONFreeString.

Little-endian. HarmonyOS arm64 and the host tests both are.

Load:
  (lldb) command script import <repo>/core-render-ohos/src/main/cpp/libohos_render/utils/json/krjson_lldb.py

Live samples: KRJSONLldbInspect() in ohosApp/entry/src/main/cpp/napi_init.cpp.
Break on the g_krjson_lldb_inspect_sink assignment.
"""

from __future__ import print_function

import math
import re
import struct
import sys

try:
    import lldb
except ImportError:
    lldb = None

MASK64 = (1 << 64) - 1
INVALID = 0x08

TAG_NULL = 0
TAG_BOOL = 1
TAG_NUMBER = 2
TAG_STRING = 3
TAG_ARRAY = 4
TAG_OBJECT = 5
TAG_BYTES = 6
TAG_EXT = 7

IMM_BIT = 1 << 3

KIND_INT32 = 1
KIND_INT64 = 2
KIND_UINT32 = 3
KIND_UINT64 = 4
KIND_FLOAT = 5
KIND_DOUBLE = 6

KIND_LABEL = {
    KIND_INT32: "int32",
    KIND_INT64: "int64",
    KIND_UINT32: "uint32",
    KIND_UINT64: "uint64",
    KIND_FLOAT: "float",
    KIND_DOUBLE: "double",
}

STR_UTF8 = 0
STR_UTF16 = 1
STRING_HEADER = 12  # sizeof(StringBox); payload follows
BYTES_HEADER = 8

MAX_TEXT_UNITS = 48
MAX_BYTE_PREVIEW = 8
DUMP_MAX_BYTES = 1 << 20
# ArrayBox / ObjectBox: int32 rc, pad to 8, then a libc++ vector (begin, end, cap).
# Object elements are OHOS libc++ ABI 1 std::pair<std::u16string, KRJSON>, 32 bytes.
ARRAY_VECTOR_OFFSET = 8
ARRAY_CHILD_LIMIT = 128
OBJECT_STRIDE = 32
OBJECT_VALUE_OFFSET = 24

_LITERAL = re.compile(r"[+-]?(?:0x[0-9a-fA-F]+|\d+)\Z")

HELP = (
    "用法:\n"
    "  krjson <expr>        只读内存，打印类型和值\n"
    "  krjson dump <expr>   在进程内调用 KRJSONDump，打印 JSON 文本\n"
    "expr 可以是变量名、表达式，或原始整数（十进制 / 0x 十六进制）。\n"
    "dump 会在 App 进程里执行代码。先用 krjson 看摘要，确认是合法值再 dump。"
)


def as_i64(word):
    word &= MASK64
    if word >= (1 << 63):
        return word - (1 << 64)
    return word


def imm_signed(word):
    """Match `static_cast<int64_t>(v) >> 7` in EncodeImmNumber's inverse."""
    return as_i64(word) >> 7


def bits_to_float(bits):
    return struct.unpack("<f", struct.pack("<I", bits & 0xFFFFFFFF))[0]


def bits_to_double(bits):
    return struct.unpack("<d", struct.pack("<Q", bits & MASK64))[0]


def fmt_real(value):
    if math.isnan(value):
        return "nan"
    if math.isinf(value):
        return "-inf" if value < 0 else "inf"
    text = format(value, ".9g")
    if "e" not in text and "E" not in text and "." not in text:
        text += ".0"
    return text


def quote(text, truncated):
    out = ['"']
    for ch in text:
        code = ord(ch)
        if ch == "\\" or ch == '"':
            out.append("\\" + ch)
        elif ch == "\n":
            out.append("\\n")
        elif ch == "\r":
            out.append("\\r")
        elif ch == "\t":
            out.append("\\t")
        elif code < 32 or code == 0x7F:
            out.append("\\u%04x" % code)
        elif code > 0xFFFF:
            out.append(ch)
        elif 0xD800 <= code <= 0xDFFF:
            out.append("\\u%04x" % code)
        else:
            out.append(ch)
    if truncated:
        out.append("...")
    out.append('"')
    return "".join(out)


def _read(mem, addr, size):
    if mem is None or addr <= 0 or size < 0:
        return None
    if size == 0:
        return b""
    try:
        data = mem.read(addr, size)
    except Exception:
        return None
    if data is None or len(data) != size:
        return None
    return data


def _unavailable(label, ptr, mem):
    why = "no memory" if mem is None else "unreadable"
    return "%s @0x%x (%s)" % (label, ptr, why)


def summarize_imm_number(word):
    kind = (word >> 4) & 7
    label = KIND_LABEL.get(kind)
    if label is None:
        return "number kind=%d payload=%d" % (kind, imm_signed(word))
    if kind == KIND_FLOAT or kind == KIND_DOUBLE:
        # Immediate double stores float bits when the value round-trips through float.
        return "%s %s" % (label, fmt_real(bits_to_float(word >> 7)))
    if kind == KIND_UINT32 or kind == KIND_UINT64:
        return "%s %d" % (label, imm_signed(word) & MASK64)
    return "%s %d" % (label, imm_signed(word))


def summarize_heap_number(word, mem):
    ptr = word & ~0xF
    data = _read(mem, ptr, 17)
    if data is None:
        return _unavailable("number", ptr, mem)
    rc = struct.unpack_from("<i", data, 0)[0]
    bits = struct.unpack_from("<Q", data, 8)[0]
    kind = data[16]
    label = KIND_LABEL.get(kind)
    if label is None:
        return "number kind=%d bits=0x%x rc=%d" % (kind, bits, rc)
    if kind == KIND_FLOAT:
        value = fmt_real(bits_to_float(bits))
    elif kind == KIND_DOUBLE:
        value = fmt_real(bits_to_double(bits))
    elif kind == KIND_UINT32 or kind == KIND_UINT64:
        value = "%d" % bits
    else:
        value = "%d" % as_i64(bits)
    return "%s %s rc=%d" % (label, value, rc)


def _decode_text(payload, encoding):
    if encoding == STR_UTF16:
        return payload.decode("utf-16-le", "replace")
    return payload.decode("utf-8", "replace")


def summarize_string(word, mem):
    ptr = word & ~7
    header = _read(mem, ptr, STRING_HEADER)
    if header is None:
        return _unavailable("string", ptr, mem)
    rc = struct.unpack_from("<i", header, 0)[0]
    length = struct.unpack_from("<I", header, 4)[0]
    encoding = header[8]
    if encoding == STR_UTF16:
        label = "u16string"
        unit_size = 2
    elif encoding == STR_UTF8:
        label = "string"
        unit_size = 1
    else:
        label = "string enc=%d" % encoding
        unit_size = 1
    take = length if length <= MAX_TEXT_UNITS else MAX_TEXT_UNITS
    payload = _read(mem, ptr + STRING_HEADER, take * unit_size)
    if payload is None:
        return "%s len=%d rc=%d @0x%x (unreadable)" % (label, length, rc, ptr)
    text = _decode_text(payload, encoding if encoding == STR_UTF16 else STR_UTF8)
    shown = quote(text, length > MAX_TEXT_UNITS)
    if length > MAX_TEXT_UNITS:
        return "%s %s len=%d rc=%d" % (label, shown, length, rc)
    return "%s %s rc=%d" % (label, shown, rc)


def summarize_bytes(word, mem):
    ptr = word & ~7
    header = _read(mem, ptr, BYTES_HEADER)
    if header is None:
        return _unavailable("bytes", ptr, mem)
    rc, length = struct.unpack_from("<iI", header, 0)
    if length == 0:
        return "bytes len=0 rc=%d" % rc
    take = length if length <= MAX_BYTE_PREVIEW else MAX_BYTE_PREVIEW
    payload = _read(mem, ptr + BYTES_HEADER, take)
    if payload is None:
        return "bytes len=%d rc=%d @0x%x (unreadable)" % (length, rc, ptr)
    hexpart = " ".join("%02x" % b for b in payload)
    if length > MAX_BYTE_PREVIEW:
        hexpart += " ..."
    return "bytes len=%d rc=%d [%s]" % (length, rc, hexpart)


def array_span(word, mem):
    """Return (child words, total length, first element address), or None."""
    ptr = word & ~7
    header = _read(mem, ptr + ARRAY_VECTOR_OFFSET, 16)
    if header is None:
        return None
    begin, end = struct.unpack_from("<QQ", header, 0)
    if begin == 0 and end == 0:
        return [], 0, 0
    if begin == 0 or end < begin or (end - begin) % 8 != 0:
        return None
    count = (end - begin) // 8
    if count > (1 << 20):
        return None
    take = count if count <= ARRAY_CHILD_LIMIT else ARRAY_CHILD_LIMIT
    if take == 0:
        return [], count, begin
    raw = _read(mem, begin, take * 8)
    if raw is None:
        return None
    words = list(struct.unpack_from("<%dQ" % take, raw, 0))
    return words, count, begin


def _vector_bounds(word, mem, stride):
    ptr = word & ~7
    header = _read(mem, ptr + ARRAY_VECTOR_OFFSET, 16)
    if header is None:
        return None
    begin, end = struct.unpack_from("<QQ", header, 0)
    if begin == 0 and end == 0:
        return 0, 0, 0
    if begin == 0 or end < begin or (end - begin) % stride != 0:
        return None
    count = (end - begin) // stride
    if count > (1 << 20):
        return None
    return begin, end, count


def u16string_at(mem, addr):
    """OHOS libc++ ABI 1 basic_string<char16_t>. None if the header cannot be read."""
    header = _read(mem, addr, 24)
    if header is None:
        return None
    if header[0] & 1:
        size = struct.unpack_from("<Q", header, 8)[0]
        ptr = struct.unpack_from("<Q", header, 16)[0]
        if size > (1 << 20) or ptr == 0:
            return None
        raw = _read(mem, ptr, size * 2)
        if raw is None:
            return None
    else:
        size = header[0] >> 1
        if size >= 11:
            return None
        raw = header[2 : 2 + size * 2]
    try:
        return raw.decode("utf-16-le", "replace")
    except Exception:
        return None


def object_fields(word, mem):
    """Return ([(key, value_address), ...], total count) or None."""
    bounds = _vector_bounds(word, mem, OBJECT_STRIDE)
    if bounds is None:
        return None
    begin, _end, count = bounds
    if count == 0:
        return [], 0
    take = count if count <= ARRAY_CHILD_LIMIT else ARRAY_CHILD_LIMIT
    fields = []
    seen = {}
    for i in range(take):
        elem = begin + i * OBJECT_STRIDE
        key = u16string_at(mem, elem)
        if not key:
            key = "[%d]" % i
        used = seen.get(key, 0)
        seen[key] = used + 1
        if used:
            key = "%s#%d" % (key, used)
        fields.append((key, elem + OBJECT_VALUE_OFFSET))
    return fields, count


def summarize_array(word, mem):
    ptr = word & ~7
    rc_data = _read(mem, ptr, 4)
    if rc_data is None:
        return _unavailable("array", ptr, mem)
    rc = struct.unpack_from("<i", rc_data, 0)[0]
    span = array_span(word, mem)
    if span is None:
        return "array rc=%d @0x%x" % (rc, ptr)
    count = span[1]
    return "array len=%d rc=%d" % (count, rc)


def summarize_object(word, mem):
    ptr = word & ~7
    rc_data = _read(mem, ptr, 4)
    if rc_data is None:
        return _unavailable("object", ptr, mem)
    rc = struct.unpack_from("<i", rc_data, 0)[0]
    fields = object_fields(word, mem)
    if fields is None:
        return "object rc=%d @0x%x" % (rc, ptr)
    return "object len=%d rc=%d" % (fields[1], rc)


def summarize_container(label, word, mem):
    ptr = word & ~7
    data = _read(mem, ptr, 4)
    if data is None:
        return _unavailable(label, ptr, mem)
    rc = struct.unpack_from("<i", data, 0)[0]
    return "%s rc=%d @0x%x" % (label, rc, ptr)


def summarize_ext(word, mem):
    ptr = word & ~7
    data = _read(mem, ptr, 24)
    if data is None:
        return _unavailable("ext", ptr, mem)
    rc = struct.unpack_from("<i", data, 0)[0]
    a = struct.unpack_from("<Q", data, 8)[0]
    b = struct.unpack_from("<Q", data, 16)[0]
    return "ext rc=%d a=0x%x b=0x%x" % (rc, a, b)


def summarize(word, mem):
    """One-line description of a KRJSON word. `mem.read(addr, size)` or None."""
    word &= MASK64
    if word == 0:
        return "null"
    if word == INVALID:
        return "invalid"
    tag = word & 7
    if tag == TAG_NULL:
        return "null payload=0x%x" % word
    if tag == TAG_BOOL:
        text = "bool true" if ((word >> 3) & 1) else "bool false"
        if word & ~0x9:
            text += " raw=0x%x" % word
        return text
    if tag == TAG_NUMBER:
        if word & IMM_BIT:
            return summarize_imm_number(word)
        return summarize_heap_number(word, mem)
    if tag == TAG_STRING:
        return summarize_string(word, mem)
    if tag == TAG_ARRAY:
        return summarize_array(word, mem)
    if tag == TAG_OBJECT:
        return summarize_object(word, mem)
    if tag == TAG_BYTES:
        return summarize_bytes(word, mem)
    if tag == TAG_EXT:
        return summarize_ext(word, mem)
    return "unknown tag=%d raw=0x%x" % (tag, word)


def parse_literal(text):
    text = text.strip()
    if not _LITERAL.match(text):
        return None
    try:
        return int(text, 0) & MASK64
    except ValueError:
        return None


def split_command(command):
    text = (command or "").strip()
    if text == "dump":
        return True, ""
    if text.startswith("dump "):
        return True, text[5:].strip()
    if text in ("help", "-h", "--help"):
        return False, ""
    return False, text


class _BlobMemory(object):
    def __init__(self, blobs):
        self._blobs = list(blobs)

    def read(self, addr, size):
        for base, blob in self._blobs:
            if base <= addr and addr + size <= base + len(blob):
                off = addr - base
                return bytes(blob[off : off + size])
        return None


# ---- LLDB glue (importing this file outside LLDB still loads the decoder) ----


class KRJSONSynthetic(object):
    """Expand a KRJSON array or object. Children are the element or field values."""

    def __init__(self, valobj, internal_dict):
        self.valobj = valobj
        self.names = []
        self.addrs = []

    def update(self):
        self.names = []
        self.addrs = []
        try:
            word = _load_word(self.valobj)
            if word is None:
                return False
            tag = word & 7
            mem = _memory_from_valobj(self.valobj)
            if tag == TAG_ARRAY:
                span = array_span(word, mem)
                if span is None or span[2] == 0 or span[1] <= 0:
                    return False
                shown = span[1] if span[1] <= ARRAY_CHILD_LIMIT else ARRAY_CHILD_LIMIT
                self.names = ["[%d]" % i for i in range(shown)]
                self.addrs = [span[2] + i * 8 for i in range(shown)]
            elif tag == TAG_OBJECT:
                fields = object_fields(word, mem)
                if fields is None or not fields[0]:
                    return False
                self.names = [name for name, _addr in fields[0]]
                self.addrs = [addr for _name, addr in fields[0]]
        except Exception:
            self.names = []
            self.addrs = []
        return False

    def num_children(self):
        return len(self.addrs)

    def has_children(self):
        return len(self.addrs) > 0

    def get_child_index(self, name):
        try:
            return self.names.index(name)
        except ValueError:
            return -1

    def get_child_at_index(self, index):
        # Point at the value in the process. Building an SBData from a Python
        # int fails on DevEco LLDB when bit 63 is set (int32 -1 and similar).
        if lldb is None or index < 0 or index >= len(self.addrs):
            return None
        return self.valobj.CreateValueFromAddress(
            self.names[index], self.addrs[index], self.valobj.GetType()
        )


class _ProcessMemory(object):
    def __init__(self, process):
        self._process = process

    def read(self, addr, size):
        if lldb is None or self._process is None or not self._process.IsValid():
            return None
        error = lldb.SBError()
        try:
            data = self._process.ReadMemory(addr, size, error)
        except Exception:
            data = None
            error = lldb.SBError()
        raw = _as_bytes(data, size)
        if error.Success() and raw is not None:
            return raw
        return _read_integers(self._process, addr, size)


def _as_bytes(data, size):
    """Turn an LLDB memory read into exactly `size` bytes. None if it was truncated."""
    if data is None:
        return None
    if isinstance(data, bytes):
        raw = data
    elif isinstance(data, str):
        raw = data.encode("latin-1", "replace")
    else:
        try:
            raw = bytes(bytearray(data))
        except Exception:
            return None
    if len(raw) != size:
        return None
    return raw


def _read_integers(process, addr, size):
    """Read memory as little-endian integers. Survives a byte API that stops at NUL."""
    if lldb is None or process is None:
        return None
    out = bytearray()
    cur = addr
    left = size
    while left:
        n = 8 if left >= 8 else (4 if left >= 4 else 1)
        error = lldb.SBError()
        try:
            value = process.ReadUnsignedFromMemory(cur, n, error)
        except Exception:
            return None
        if not error.Success():
            return None
        out += (int(value) & ((1 << (8 * n)) - 1)).to_bytes(n, "little")
        cur += n
        left -= n
    return bytes(out)


def _unsigned_from_value(valobj):
    if valobj is None or not valobj.IsValid():
        return None
    if valobj.GetValueAsUnsigned(0) == valobj.GetValueAsUnsigned(1):
        return valobj.GetValueAsUnsigned(0) & MASK64
    data = valobj.GetData()
    if data is not None and data.IsValid() and data.GetByteSize() >= 8:
        error = lldb.SBError()
        word = data.GetUnsignedInt64(error, 0)
        if error.Success():
            return word & MASK64
    text = valobj.GetValue()
    if text:
        literal = parse_literal(text)
        if literal is not None:
            return literal
    if valobj.GetNumChildren() == 1:
        return _unsigned_from_value(valobj.GetChildAtIndex(0))
    return None


def _load_word(valobj):
    current = valobj
    for _ in range(2):
        if current is None or not current.IsValid():
            return None
        type_ = current.GetType().GetCanonicalType()
        if type_.IsPointerType() or type_.IsReferenceType():
            current = current.Dereference()
            continue
        break
    return _unsigned_from_value(current)


def _memory_from_valobj(valobj):
    process = valobj.GetProcess()
    if process is None or not process.IsValid():
        return None
    return _ProcessMemory(process)


def krjson_summary(valobj, internal_dict):
    try:
        word = _load_word(valobj)
        if word is None:
            return "<krjson: not an integer>"
        return summarize(word, _memory_from_valobj(valobj))
    except Exception as exc:
        return "<krjson error: %s>" % exc


def _expression_options():
    options = lldb.SBExpressionOptions()
    options.SetLanguage(lldb.eLanguageTypeC)
    options.SetIgnoreBreakpoints(True)
    options.SetUnwindOnError(True)
    options.SetTryAllThreads(False)
    options.SetStopOthers(True)
    options.SetSuppressPersistentResult(True)
    options.SetTimeoutInMicroSeconds(3000000)
    return options


def _first_load_address(target, sc_list):
    if sc_list is None:
        return None
    invalid = lldb.LLDB_INVALID_ADDRESS
    for i in range(sc_list.GetSize()):
        ctx = sc_list.GetContextAtIndex(i)
        for getter in (ctx.GetSymbol, ctx.GetFunction):
            obj = getter()
            if obj is None or not obj.IsValid():
                continue
            start = obj.GetStartAddress()
            if start is None or not start.IsValid():
                continue
            load = start.GetLoadAddress(target)
            if load != invalid and load != 0:
                return load
    return None


def _find_code_address(target, name):
    if target is None or not target.IsValid():
        return None
    found = target.FindFunctions(name, lldb.eFunctionNameTypeAuto)
    addr = _first_load_address(target, found)
    if addr is not None:
        return addr
    code = getattr(lldb, "eSymbolTypeCode", 0)
    addr = _first_load_address(target, target.FindSymbols(name, code))
    if addr is not None:
        return addr
    return _first_load_address(target, target.FindSymbols(name))


def _eval(frame, expr):
    value = frame.EvaluateExpression(expr, _expression_options())
    error = value.GetError()
    if not value.IsValid() or (error.IsValid() and error.Fail()):
        message = error.GetCString() if error.IsValid() else None
        return None, message or "expression failed"
    return value, None


def _eval_word(frame, expr):
    value, error = _eval(frame, expr)
    if error is not None:
        return None, error
    word = _unsigned_from_value(value)
    if word is None:
        return None, "expression is not a 64-bit integer (%s)" % value.GetTypeName()
    return word, None


def _dump_word(frame, word):
    target = frame.GetThread().GetProcess().GetTarget()
    dump_addr = _find_code_address(target, "KRJSONDump")
    free_addr = _find_code_address(target, "KRJSONFreeString")
    if dump_addr is None:
        return None, "找不到 KRJSONDump。确认 libohos_render.so 已加载，且 debug 包带符号。"
    expr = "((char *(*)(unsigned long long))0x%x)((unsigned long long)0x%016xULL)" % (
        dump_addr,
        word,
    )
    value, error = _eval(frame, expr)
    if error is not None:
        return None, error
    ptr = value.GetValueAsUnsigned(0)
    if value.GetValueAsUnsigned(0) != value.GetValueAsUnsigned(1):
        return None, "KRJSONDump 返回值不是指针"
    if ptr == 0:
        return None, "KRJSONDump 返回 NULL"
    process = frame.GetProcess()
    read_error = lldb.SBError()
    text = process.ReadCStringFromMemory(ptr, DUMP_MAX_BYTES, read_error)
    free_note = None
    if free_addr is None:
        free_note = "找不到 KRJSONFreeString，进程里仍留着 %s" % hex(ptr)
    else:
        free_expr = "((void (*)(char *))0x%x)((char *)0x%x)" % (free_addr, ptr)
        _, free_error = _eval(frame, free_expr)
        if free_error is not None:
            free_note = "KRJSONFreeString 失败: %s" % free_error
    if text is None:
        text = ""
    if not read_error.Success() and text == "":
        message = read_error.GetCString() or "无法读出 KRJSONDump 的结果"
        if free_note:
            message += "\n" + free_note
        return None, message
    if free_note:
        text = text + "\n(" + free_note + ")"
    elif len(text) >= DUMP_MAX_BYTES - 1:
        text += "\n(truncated)"
    return text, None


def _resolve_word(frame, expr):
    literal = parse_literal(expr)
    if literal is not None:
        return literal, None
    if frame is None or not frame.IsValid():
        return None, "没有选中的栈帧，无法求值。可以改传 0x 十六进制原值。"
    return _eval_word(frame, expr)


def krjson_command(debugger, command, exe_ctx, result, internal_dict):
    try:
        _krjson_command(command, exe_ctx, result)
    except Exception as exc:
        result.SetError("krjson: %s" % exc)


def _krjson_command(command, exe_ctx, result):
    dump, expr = split_command(command)
    if not expr:
        text = (command or "").strip()
        if dump or text not in ("", "help", "-h", "--help"):
            result.SetError(HELP)
        else:
            result.AppendMessage(HELP)
        return
    frame = exe_ctx.GetFrame() if exe_ctx is not None else None
    word, error = _resolve_word(frame, expr)
    if error is not None:
        result.SetError(error)
        return
    if dump:
        if frame is None or not frame.IsValid():
            result.SetError("没有选中的栈帧，无法调用 KRJSONDump。")
            return
        text, error = _dump_word(frame, word)
        if error is not None:
            result.SetError(error)
            return
        result.AppendMessage(text)
        return
    process = frame.GetProcess() if frame is not None and frame.IsValid() else None
    mem = _ProcessMemory(process) if process is not None and process.IsValid() else None
    result.AppendMessage(summarize(word, mem))
    result.AppendMessage("raw 0x%016x" % word)


def _quiet(debugger, command):
    ret = lldb.SBCommandReturnObject()
    debugger.GetCommandInterpreter().HandleCommand(command, ret)
    return ret


def __lldb_init_module(debugger, internal_dict):
    _quiet(debugger, "command script delete krjson")
    added = _quiet(
        debugger,
        "command script add -f krjson_lldb.krjson_command "
        "-h 'Decode a KRJSON uint64 (see krjson help)' krjson",
    )
    for name in ("KRJSON", "::KRJSON"):
        _quiet(
            debugger,
            "type summary add -w KRJSON -v -e -F krjson_lldb.krjson_summary %s" % name,
        )
        _quiet(
            debugger,
            "type synthetic add -w KRJSON -l krjson_lldb.KRJSONSynthetic %s" % name,
        )
    _quiet(debugger, "type category enable KRJSON")
    if added.Succeeded():
        print("krjson_lldb: loaded, arrays expand")
    else:
        print("krjson_lldb: command install failed: %s" % (added.GetError() or ""))


def _encode_imm(kind, payload):
    """Test twin of EncodeImmNumber. Not used by the debugger."""
    return (((payload & MASK64) << 7) & MASK64) | ((kind & 7) << 4) | TAG_NUMBER | IMM_BIT


def _expect(failures, actual, expected):
    if actual != expected:
        failures.append("%r != %r" % (actual, expected))


def run_self_test():
    failures = []
    expect = lambda actual, expected: _expect(failures, actual, expected)

    expect(_encode_imm(KIND_INT32, MASK64), 0xFFFFFFFFFFFFFF9A)
    expect(summarize(0, None), "null")
    expect(summarize(INVALID, None), "invalid")
    expect(summarize(0x10, None), "null payload=0x10")
    expect(summarize(0x1, None), "bool false")
    expect(summarize(0x9, None), "bool true")
    expect(summarize(0x19, None), "bool true raw=0x19")
    expect(summarize(_encode_imm(KIND_INT32, MASK64), None), "int32 -1")
    expect(summarize(_encode_imm(KIND_INT64, (-(1 << 56)) & MASK64), None), "int64 -72057594037927936")
    expect(summarize(_encode_imm(KIND_UINT32, 0), None), "uint32 0")
    expect(summarize(_encode_imm(KIND_UINT64, (1 << 56) - 1), None), "uint64 72057594037927935")
    expect(summarize(_encode_imm(KIND_FLOAT, struct.unpack("<I", struct.pack("<f", 1.5))[0]), None), "float 1.5")
    expect(summarize(_encode_imm(KIND_DOUBLE, struct.unpack("<I", struct.pack("<f", 0.5))[0]), None), "double 0.5")

    string = bytearray(STRING_HEADER + 5)
    struct.pack_into("<iI", string, 0, 2, 5)
    string[8] = STR_UTF8
    string[STRING_HEADER:] = b"hello"
    u16 = bytearray(STRING_HEADER + 2)
    struct.pack_into("<iI", u16, 0, 1, 1)
    u16[8] = STR_UTF16
    u16[STRING_HEADER : STRING_HEADER + 2] = b"\x60\x4f"
    raw = bytearray(BYTES_HEADER + 3)
    struct.pack_into("<iI", raw, 0, 4, 3)
    raw[BYTES_HEADER:] = b"\x0a\x0b\x0c"
    array = struct.pack("<i", 2)
    number = bytearray(32)
    struct.pack_into("<i", number, 0, 3)
    struct.pack_into("<Q", number, 8, struct.unpack("<Q", struct.pack("<d", 2.5))[0])
    number[16] = KIND_DOUBLE
    heap_int = bytearray(32)
    struct.pack_into("<i", heap_int, 0, 1)
    struct.pack_into("<Q", heap_int, 8, as_i64(-2) & MASK64)
    heap_int[16] = KIND_INT64
    ext = bytearray(24)
    struct.pack_into("<i", ext, 0, 1)
    struct.pack_into("<Q", ext, 8, 0x11)
    struct.pack_into("<Q", ext, 16, 0x22)
    long_text = bytearray(STRING_HEADER + 60)
    struct.pack_into("<iI", long_text, 0, 1, 60)
    long_text[8] = STR_UTF8
    long_text[STRING_HEADER:] = b"a" * 60

    mem = _BlobMemory(
        [
            (0x1000, string),
            (0x3000, u16),
            (0x4000, raw),
            (0x5000, array),
            (0x6000, ext),
            (0x7000, number),
            (0x7100, heap_int),
            (0x8000, long_text),
        ]
    )
    expect(summarize(0x1000 | TAG_STRING, mem), 'string "hello" rc=2')
    expect(summarize(0x3000 | TAG_STRING, mem), 'u16string "\u4f60" rc=1')
    expect(summarize(0x4000 | TAG_BYTES, mem), "bytes len=3 rc=4 [0a 0b 0c]")
    expect(summarize(0x5000 | TAG_ARRAY, mem), "array rc=2 @0x5000")
    array_box = bytearray(32)
    struct.pack_into("<i", array_box, 0, 1)
    struct.pack_into("<QQ", array_box, 8, 0xA000, 0xA000 + 16)
    array_elems = struct.pack("<QQ", 0x1, _encode_imm(KIND_INT32, MASK64))
    span_mem = _BlobMemory([(0x6000, bytes(array_box)), (0xA000, array_elems)])
    # 0x6000 is also used as an ext box above; this memory object is separate.
    expect(array_span(0x6100 | TAG_ARRAY, span_mem), None)
    got_span = array_span(0x6000 | TAG_ARRAY, span_mem)
    expect(got_span[1] if got_span else None, 2)
    expect(got_span[0][1] if got_span else None, _encode_imm(KIND_INT32, MASK64))
    expect(summarize(0x6000 | TAG_ARRAY, span_mem), "array len=2 rc=1")
    expect(summarize(0x5008 | TAG_OBJECT, None), "object @0x5008 (no memory)")
    expect(summarize(0x6000 | TAG_EXT, mem), "ext rc=1 a=0x11 b=0x22")
    expect(summarize(0x7000 | TAG_NUMBER, mem), "double 2.5 rc=3")
    expect(summarize(0x7100 | TAG_NUMBER, mem), "int64 -2 rc=1")
    expect(summarize(0x9000 | TAG_STRING, mem), "string @0x9000 (unreadable)")
    long_summary = summarize(0x8000 | TAG_STRING, mem)
    expect(long_summary.startswith('string "'), True)
    expect("len=60" in long_summary and long_summary.endswith("rc=1"), True)
    expect(long_summary.count("a"), MAX_TEXT_UNITS)

    expect(parse_literal("0x9"), 9)
    expect(parse_literal("-1"), MASK64)
    expect(parse_literal("bits"), None)
    expect(split_command("dump bits"), (True, "bits"))
    expect(split_command("  bits  "), (False, "bits"))
    expect(split_command("dump"), (True, ""))
    expect(split_command("help"), (False, ""))
    age = bytes.fromhex(
        "065c6100670065000000fdc47f000000085cfdc47f0000009a01000000000000"
    )
    ni = bytes.fromhex(
        "02ec604f000000009a010000000000001cc0269c7f00000013c0269c7f000000"
    )
    obj_box = bytearray(32)
    struct.pack_into("<i", obj_box, 0, 1)
    struct.pack_into("<QQ", obj_box, 8, 0xC000, 0xC000 + 64)
    obj_mem = _BlobMemory([(0xD000, bytes(obj_box)), (0xC000, age + ni)])
    fields = object_fields(0xD000 | TAG_OBJECT, obj_mem)
    expect(fields[1] if fields else None, 2)
    expect(fields[0][0][0] if fields else None, "age")
    expect(fields[0][1][0] if fields else None, "\u4f60")
    expect(summarize(0xD000 | TAG_OBJECT, obj_mem), "object len=2 rc=1")
    expect(summarize(struct.unpack_from("<Q", age, 24)[0], obj_mem), "int32 3")
    return failures


if __name__ == "__main__":
    failed = run_self_test()
    if failed:
        print("%d failed" % len(failed))
        for item in failed:
            print(item)
        sys.exit(1)
    print("ok")
