#!/usr/bin/python

##
# Code copied from https://git.woa.com/jiamiaohe/kotlin(commit SHA:6bd90f0f863f9b6eb64d7438d3a32413c9da98dd on 11/11/2024), 
# writed by saiyanren based on JetBrains, copyright (c) 2024 Tencent all rights reserved for the modifications. Do not edit.
#

##
# Copyright 2010-2017 JetBrains s.r.o.
# 
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#


import lldb
import struct
import re
import sys
import os
import time
import io
import traceback
import datetime
from enum import Enum

NULL = 'null'
logging=True
exe_logging=True
bench_logging=False
f = None

class CachedDictType(Enum):
    READ_STRING = 1
    LOADED_ADDRESS = 2
    TYPE_INFO_EXPR = 3

def log(msg):
    if logging:
    #     sys.stderr.write(msg())
    #     sys.stderr.write('\n')
        current_time = datetime.datetime.now().strftime("%m-%d %H:%M:%S.%f")[:-3]
        n_msg = current_time + " " + str(msg())
        exelog(lambda: n_msg)

def exelog(stmt):
    if exe_logging:
        global f
        f.write(stmt() + "\n")
        f.flush()

def bench(start, msg):
    if bench_logging:
        print("{}: {}".format(msg(), time.monotonic() - start))

def evaluate(expr):
    log(lambda : "evaluate begin:  {}".format(expr))
    target = lldb.debugger.GetSelectedTarget()
    result = target.EvaluateExpression(expr)
    log(lambda : "evaluate end:  {} => {}".format(expr, result))
    return result


class DebuggerException(Exception):
    pass


_OUTPUT_MAX_CHILDREN = re.compile(r"target.max-children-count \(int\) = (.*)\n")
def _max_children_count():
    log(lambda : "_max_children_count begin")
    result = lldb.SBCommandReturnObject()
    lldb.debugger.GetCommandInterpreter().HandleCommand("settings show target.max-children-count", result, False)
    if not result.Succeeded():
        raise DebuggerException()
    v = _OUTPUT_MAX_CHILDREN.search(result.GetOutput()).group(1)
    log(lambda : "_max_children_count end")
    return int(v)


def _symbol_loaded_address(name, debugger = lldb.debugger):
    log(lambda : "_symbol_loaded_address begin {}".format(name))
    loaded_address = get_value_from_cached_dict(CachedDictType.LOADED_ADDRESS,name)
    if loaded_address is not None:
        log(lambda: "_symbol_loaded_address: get address from cache")
        return loaded_address

    target = debugger.GetSelectedTarget()
    log(lambda : "_symbol_loaded_address get target end")
    frame = target.GetProcess().GetSelectedThread().GetSelectedFrame()
    log(lambda : "_symbol_loaded_address get frame end")
    candidates = list(filter(lambda x: x.name == name, frame.module.symbols))
    # take first
    for candidate in candidates:
        address = candidate.GetStartAddress().GetLoadAddress(target)
        log(lambda: "_symbol_loaded_address: get address from frame {} {:#x}".format(name, address))
        add_to_cached_dict(CachedDictType.LOADED_ADDRESS,name,address)
        return address

def _type_info_by_address(address, debugger = lldb.debugger):
    log(lambda : "_type_info_by_address begin {}".format(address))
    target = debugger.GetSelectedTarget()
    log(lambda : "_type_info_by_address get target end")
    frame = target.GetProcess().GetSelectedThread().GetSelectedFrame()
    log(lambda : "_type_info_by_address get frame end")
    candidates = list(filter(lambda x: x.GetStartAddress().GetLoadAddress(target) == address, frame.module.symbols))
    log(lambda : "_type_info_by_address end")
    return candidates

def is_instance_of(addr, typeinfo):
    return evaluate("(bool)Konan_DebugIsInstance({:#x}, {:#x})".format(addr, typeinfo)).GetValue() == "true"

def type_info(value):
    """This method checks self-referencing of pointer of first member of TypeInfo including case when object has an
    meta-object pointed by TypeInfo. Two lower bits are reserved for memory management needs see runtime/src/main/cpp/Memory.h."""
    log(lambda: "type_info begin ({:#x}: {})".format(value.unsigned, value.GetTypeName()))
    if value.GetTypeName() != "ObjHeader *":
        log(lambda: "type_info value.GetTypeName() != ObjHeader *")
        return None
    expr = "*(void **)((uintptr_t)(*(void**){0:#x}) & ~0x3) == **(void***)((uintptr_t)(*(void**){0:#x}) & ~0x3) " \
           "? *(void **)((uintptr_t)(*(void**){0:#x}) & ~0x3) : (void *)0".format(value.unsigned)
    log(lambda: "type_info will evaluate(expr: {})".format(expr))
    result = get_value_from_cached_dict(CachedDictType.TYPE_INFO_EXPR,expr)
    if result is not None:
        log(lambda: "type_info result from cache")
    else:
        result = evaluate(expr)
        add_to_cached_dict(CachedDictType.TYPE_INFO_EXPR,expr,result)
        log(lambda: "type_info result from evaluate")

    log(lambda: "type_info end and the result: ({})".format(result))
    return result.unsigned if result.IsValid() and result.unsigned != 0 else None


__FACTORY = {}


# Cache type info pointer to [ChildMetaInfo]
SYNTHETIC_OBJECT_LAYOUT_CACHE = {}
TO_STRING_DEPTH = 2
ARRAY_TO_STRING_LIMIT = 10

_TYPE_CONVERSION = [
     lambda obj, value, address, name: value.CreateValueFromExpression(name, "(void *){:#x}".format(address)),
     lambda obj, value, address, name: value.CreateValueFromAddress(name, address, value.type),
     lambda obj, value, address, name: value.CreateValueFromExpression(name, "(int8_t *){:#x}".format(address)),
     lambda obj, value, address, name: value.CreateValueFromExpression(name, "(int16_t *){:#x}".format(address)),
     lambda obj, value, address, name: value.CreateValueFromExpression(name, "(int32_t *){:#x}".format(address)),
     lambda obj, value, address, name: value.CreateValueFromExpression(name, "(int64_t *){:#x}".format(address)),
     lambda obj, value, address, name: value.CreateValueFromExpression(name, "(float *){:#x}".format(address)),
     lambda obj, value, address, name: value.CreateValueFromExpression(name, "(double *){:#x}".format(address)),
     lambda obj, value, address, name: value.CreateValueFromExpression(name, "(void **){:#x}".format(address)),
     lambda obj, value, address, name: value.CreateValueFromExpression(name, "(bool *){:#x}".format(address)),
     lambda obj, value, address, name: None]

_TYPES = [
      lambda x: x.GetType().GetBasicType(lldb.eBasicTypeVoid).GetPointerType(),
      lambda x: x.GetType(),
      lambda x: x.GetType().GetBasicType(lldb.eBasicTypeChar),
      lambda x: x.GetType().GetBasicType(lldb.eBasicTypeShort),
      lambda x: x.GetType().GetBasicType(lldb.eBasicTypeInt),
      lambda x: x.GetType().GetBasicType(lldb.eBasicTypeLongLong),
      lambda x: x.GetType().GetBasicType(lldb.eBasicTypeFloat),
      lambda x: x.GetType().GetBasicType(lldb.eBasicTypeDouble),
      lambda x: x.GetType().GetBasicType(lldb.eBasicTypeVoid).GetPointerType(),
      lambda x: x.GetType().GetBasicType(lldb.eBasicTypeBool)
]

def kotlin_object_type_summary(lldb_val, internal_dict = {}):
    """Hook that is run by lldb to display a Kotlin object."""
    log(lambda: "kotlin_object_type_summary, begin ({:#x}: {})".format(lldb_val.unsigned, lldb_val.type.name))
    start = time.monotonic()

    if lldb_val.GetTypeName() != "ObjHeader *":
        if lldb_val.GetValue() is None:
            log(lambda: "kotlin_object_type_summary, not ObjHeader and none :({:#x}) = NULL".format(lldb_val.unsigned))
            bench(start, lambda: "kotlin_object_type_summary:({:#x}) = NULL".format(lldb_val.unsigned))
            return NULL
        log(lambda: "kotlin_object_type_summary, not ObjHeader and not none :({:#x}) = {}".format(lldb_val.unsigned, lldb_val.signed))
        bench(start, lambda: "kotlin_object_type_summary:({:#x}) = {}".format(lldb_val.unsigned, lldb_val.signed))
        return lldb_val.value

    if lldb_val.unsigned == 0:
            log(lambda: "kotlin_object_type_summary, unsigned equal 0 :({:#x}) = NULL".format(lldb_val.unsigned))
            bench(start, lambda: "kotlin_object_type_summary:({:#x}) = NULL".format(lldb_val.unsigned))
            return NULL
    tip = internal_dict["type_info"] if "type_info" in internal_dict.keys() else type_info(lldb_val)

    if not tip:
        log(lambda: "kotlin_object_type_summary return for tip is falsey :({0:#x}) = falback:{0:#x}".format(lldb_val.unsigned))
        bench(start, lambda: "kotlin_object_type_summary:({0:#x}) = falback:{0:#x}".format(lldb_val.unsigned))
        fallback = lldb_val.GetValue()
        return fallback

    log(lambda: "kotlin_object_type_summary will select_provider")
    value = select_provider(lldb_val, tip, internal_dict)

    log(lambda: "kotlin_object_type_summary, will toString :({:#x}) = value:{:#x}".format(lldb_val.unsigned, value._valobj.unsigned))
    bench(start, lambda: "kotlin_object_type_summary:({:#x}) = value:{:#x}".format(lldb_val.unsigned, value._valobj.unsigned))
    start = time.monotonic()
    str0 = value.to_short_string()
    log(lambda: "kotlin_object_type_summary, end :({:#x}) = str:'{}...'".format(lldb_val.unsigned, str0[:3]))

    bench(start, lambda: "kotlin_object_type_summary:({:#x}) = str:'{}...'".format(lldb_val.unsigned, str0[:3]))
    return str0


def select_provider(lldb_val, tip, internal_dict):
    start = time.monotonic()
    log(lambda : "select_provider, will is_string_or_array {:#x} name:{} tip:{:#x}".format(lldb_val.unsigned, lldb_val.name, tip))
    error = lldb.SBError()
    flow_string = _read_string("(char *)Konan_DebugCompleteTypeInitFlow({0:#x}, {1:#x})".
                               format(lldb_val.unsigned, _symbol_loaded_address('kclass:kotlin.String')),
                              error)
    if not error.Success():
        log(lambda : "Konan_DebugCompleteTypeInitFlow error:{}".format(error.GetMessage()))
        raise DebuggerException()

    flow_steps_string = flow_string.split("|")
    log(lambda : "select_provider, flow_steps_string:{}".format(flow_steps_string))
    if flow_steps_string[0] == '1':
        buff_addr = int(flow_steps_string[1])
        buff_len = int(flow_steps_string[2])
        return __FACTORY['string'](lldb_val, buff_addr, buff_len)
    elif flow_steps_string[0] == '2':
        children_count = int(flow_steps_string[1])
        return __FACTORY['array'](lldb_val, internal_dict, children_count)
    else:
        children_count = int(flow_steps_string[1])
        children = flow_steps_string[2].split(",")
        children_type_element = flow_steps_string[3]
        children_type = children_type_element.split(",")
        children_addr_element = flow_steps_string[4]
        children_addr = children_addr_element.split(",")

        log(lambda : "flow_steps_string[0] == else, children_count:{}, children:{}, children_type:{}, children_addr:{}".
            format(children_count, children, children_type, children_addr))
        return __FACTORY['object'](lldb_val, internal_dict, children_count, children, children_type, children_addr)

class KonanHelperProvider(lldb.SBSyntheticValueProvider):
    def __init__(self, valobj, amString, internal_dict = {}):
        log(lambda : "KonanHelperProvider init begin")
        self._target = lldb.debugger.GetSelectedTarget()
        self._process = self._target.GetProcess()
        self._valobj = valobj
        self._internal_dict = internal_dict.copy()
        if amString:
            return
        if self._children_count == 0:
            children_count = evaluate("(int)Konan_DebugGetFieldCount({:#x})".format(self._valobj.unsigned)).signed
            log(lambda: "(int)[{}].Konan_DebugGetFieldCount({:#x}) = {}".format(self._valobj.name,
                                                                                self._valobj.unsigned, children_count))
            self._children_count = children_count
        log(lambda : "KonanHelperProvider init end")

    def __del__(self):
        log(lambda: "KonanHelperProvider del")

    def _read_string(self, expr, error):
        return _read_string(expr, error)

    def _read_value(self, index):
        log(lambda: "_read_value begin : [{}]".format(index))
        value_type = self._field_type(index)
        address = self._field_address(index)
        log(lambda: "_read_value end : [index:{}, type:{}] and will handle _TYPE_CONVERSION : [self._valobj:{:#x}, address:{:#x}]".format(index, value_type, self._valobj.unsigned, address))
        return _TYPE_CONVERSION[int(value_type)](self, self._valobj, address, str(self._field_name(index)))

    def _read_type(self, index):
        log(lambda: "_read_type begin, index:{}".format(index))
        type = _TYPES[self._field_type(index)](self._valobj)
        log(lambda: "type:{0} of {1:#x} of {2:#x}".format(type, self._valobj.unsigned,
                                                          self._valobj.unsigned + self._children[index].offset()))
        return type

    def _deref_or_obj_summary(self, index, internal_dict = {}):
        log(lambda : "_deref_or_obj_summary begin index:{}".format(index))
        value = self._read_value(index)
        if not value:
            log(lambda : "_deref_or_obj_summary return for value is falsey, index:{}, type:{}".format(index, self._children[index].type()))
            return None
        log(lambda : "_deref_or_obj_summary end index:{}".format(index))
        return value.value if type_info(value) else value.deref.value

    def _field_address(self, index):
        if not hasattr(self, '_children_addr') or not self._children_addr:
            return evaluate("(void *)Konan_DebugGetFieldAddress({:#x}, {})".format(self._valobj.unsigned, index)).unsigned
        else:
            return int(self._children_addr[index])

    def _field_type(self, index):
        if not hasattr(self, '_children_type') or not self._children_type:
            return evaluate("(int)Konan_DebugGetFieldType({:#x}, {})".format(self._valobj.unsigned, index)).unsigned
        else:
            return self._children_type[index]

    def to_string_by_fields_name_list(self):
        log(lambda: "KonanHelperProvider to_string_by_fields_name_list begin")
        max_children_count=_max_children_count()
        limit = min(self._children_count, max_children_count)
        namelist = self._fields_name_list(limit)
        words = namelist.split('|')
        replaced_words = [word + ": ..." for word in words]
        replaced_string = ', '.join(replaced_words)
        log(lambda: "KonanHelperProvider to_string_by_fields_name_list end new:[{}]".format(replaced_string))
        return replaced_string

    def to_string(self, representation):
        log(lambda: "KonanHelperProvider to_string begin")
        writer = io.StringIO()
        max_children_count=_max_children_count()
        limit = min(self._children_count, max_children_count)
        for i in range(limit):
            writer.write(representation(i))
            if (i != limit - 1):
                writer.write(", ")
        if max_children_count < self._children_count:
            writer.write(', ...')

        log(lambda: "KonanHelperProvider to_string end:[{}]".format(writer.getvalue()))
        return "[{}]".format(writer.getvalue())


class KonanStringSyntheticProvider(KonanHelperProvider):
    def __init__(self, valobj, buff_addr, buff_len):
        log(lambda: "KonanStringSyntheticProvider valobj.unsigned:{:#x} valobj.name:{} buff_addr:{:#x} buff_len:{:#x}".format(
            valobj.unsigned, valobj.name, buff_addr, buff_len))
        self._children_count = 0
        super(KonanStringSyntheticProvider, self).__init__(valobj, True)
        fallback = valobj.GetValue()
        if not buff_len:
            self._representation = fallback
            return

        error = lldb.SBError()
        s = self._process.ReadCStringFromMemory(buff_addr, buff_len, error)
        if not error.Success():
            log(lambda: "KonanStringSyntheticProvider did handle self._process.ReadCStringFromMemory. but fail")
            raise DebuggerException()
        else:
            log(lambda: "KonanStringSyntheticProvider did handle self._process.ReadCStringFromMemory. result : {}".format(s))
        self._representation = s if error.Success() else fallback
        self._logger = lldb.formatters.Logger.Logger()

    def __del__(self):
        log(lambda: "KonanStringSyntheticProvider del")

    def update(self):
        pass

    def num_children(self):
        return 0

    def has_children(self):
        return False

    def get_child_index(self, _):
        return None

    def get_child_at_index(self, _):
        return None

    def to_short_string(self):
        log(lambda: "KonanStringSyntheticProvider to_short_string")
        return self._representation

    def to_string(self):
        return self._representation

g_num_children_handle_recorded = []

class KonanObjectSyntheticProvider(KonanHelperProvider):
    def __init__(self, valobj, internal_dict, children_count, children, children_type, children_addr):
        # Save an extra call into the process
        log(lambda: "KonanObjectSyntheticProvider init begin ({:#x})".format(valobj.unsigned))
        self._children_count = children_count
        self._children_type = children_type
        self._children_addr = children_addr
        super(KonanObjectSyntheticProvider, self).__init__(valobj, False, internal_dict)
        self._children = children
        log(lambda: "KonanObjectSyntheticProvider init end ({:#x}) _children:{}".format(self._valobj.unsigned,
                                                                                        self._children))

    def __del__(self):
        log(lambda: "KonanObjectSyntheticProvider del")

    def _field_name(self, index):
        log(lambda: "KonanObjectSyntheticProvider::_field_name begin ({:#x}, {})".format(self._valobj.unsigned, index))
        name = self._children[index]
        log(lambda: "KonanObjectSyntheticProvider::_field_name end ({:#x}, {}) = {}".format(self._valobj.unsigned,
                                                                                       index, name))
        return name

    def _fields_name_list(self, child_count):
        log(lambda: "KonanObjectSyntheticProvider::_fields_name_list begin and end ({:#x}, {})".format(self._valobj.unsigned, child_count))
        return "|".join(self._children)

    def num_children(self):
        # 因发现在二次访问同一变量的一子变量时，即使其父子内存等参数不变，获取的值却会变，且暂未找到如析构时的较好时机去清理工作链缓存，固结合实际情况，先在这
        # 里做一个处理：第二次访问此处时，清理相关缓存
        combined = [self._valobj.unsigned,self._children_count]
        global g_num_children_handle_recorded
        if combined in g_num_children_handle_recorded:
            log(lambda: "KonanObjectSyntheticProvider::num_children twice. ({:#x}) = {}".format(self._valobj.unsigned,self._children_count))
            clean_cached_dict(CachedDictType.READ_STRING)
        else:
            log(lambda: "KonanObjectSyntheticProvider::num_children({:#x}) = {}".format(self._valobj.unsigned,self._children_count))
        g_num_children_handle_recorded.append(combined)

        return self._children_count

    def has_children(self):
        log(lambda: "KonanObjectSyntheticProvider::has_children({:#x}) = {}".format(self._valobj.unsigned,
                                                                                    self._children_count > 0))
        return self._children_count > 0

    def get_child_index(self, name):
        log(lambda: "KonanObjectSyntheticProvider::get_child_index({:#x}, {})".format(self._valobj.unsigned, name))
        index = self._children.index(name)
        log(lambda: "KonanObjectSyntheticProvider::get_child_index({:#x}) index={}".format(self._valobj.unsigned,
                                                                                           index))
        return index

    def get_child_at_index(self, index):
        log(lambda: "KonanObjectSyntheticProvider::get_child_at_index({:#x}, {})".format(self._valobj.unsigned, index))
        return self._read_value(index)

    def to_short_string(self):
        log(lambda: "KonanObjectSyntheticProvider::to_short_string:{:#x}".format(self._valobj.unsigned))
        return super().to_string_by_fields_name_list()

    def to_string(self):
        log(lambda: "KonanObjectSyntheticProvider::to_string:{:#x}".format(self._valobj.unsigned))
        return super().to_string(lambda index: "{}: {}".format(self._field_name(index),
                                                               self._deref_or_obj_summary(index)))

class KonanArraySyntheticProvider(KonanHelperProvider):
    def __init__(self, valobj, internal_dict, _children_count):
        log(lambda: "KonanArraySyntheticProvider: valobj:{:#x}, _children_count:{}".format(valobj.unsigned, _children_count))
        self._children_count = _children_count
        super(KonanArraySyntheticProvider, self).__init__(valobj, False, internal_dict)
        if self._valobj is None:
            return
        valobj.SetSyntheticChildrenGenerated(True)

    def __del__(self):
        log(lambda: "KonanArraySyntheticProvider del")

    def num_children(self):
        log(lambda: "KonanArraySyntheticProvider::num_children({:#x}) = {}".format(self._valobj.unsigned,
                                                                                   self._children_count))
        return self._children_count

    def has_children(self):
        log(lambda: "KonanArraySyntheticProvider::has_children({:#x}) = {}".format(self._valobj.unsigned,
                                                                                   self._children_count> 0))
        return self._children_count > 0

    def get_child_index(self, name):
        log(lambda: "KonanArraySyntheticProvider::get_child_index({:#x}, {})".format(self._valobj.unsigned, name))
        index = int(name)
        return index if (0 <= index < self._children_count) else -1

    def get_child_at_index(self, index):
        log(lambda: "KonanArraySyntheticProvider::get_child_at_index begin ({:#x}, {})".format(self._valobj.unsigned, index))
        if not hasattr(self, '_children_type') or not self._children_type:
            # 先前未拉取过子元素的信息，则在此预拉取并做缓存以便后用（这表明调用直接来自lldb server等，此处并不可见，无法在既定埋点处处理）
            log(lambda: "KonanArraySyntheticProvider::get_child_at_index not self._children_type")
            error = lldb.SBError()
            fieldsTypeAndAddress_string = _read_string("(char *)Konan_DebugGetFieldsTypeAndAddress({0:#x}, {1:#x})".
                               format(self._valobj.unsigned, self._children_count),
                               error)
            if not error.Success():
                log(lambda : "Konan_DebugGetFieldsTypeAndAddress error:{}".format(error.GetMessage()))
                raise DebuggerException()

            fieldsTypeAndAddress_steps_string = fieldsTypeAndAddress_string.split("|")
            log(lambda : "select_provider, flow_steps_string:{}".format(fieldsTypeAndAddress_steps_string))
            children_type_element = fieldsTypeAndAddress_steps_string[0]
            self._children_type = children_type_element.split(",")

            if not hasattr(self, '_children_addr') or not self._children_addr:
                children_addr_element = fieldsTypeAndAddress_steps_string[1]
                self._children_addr = children_addr_element.split(",")
            else:
                log(lambda: "KonanArraySyntheticProvider::get_child_at_index not self._children_type but self._children_addr")
                raise DebuggerException()

            # 缓存下一级的查询流


        value = self._read_value(index)
        log(lambda: "KonanArraySyntheticProvider::get_child_at_index({})".format(value))
        return value

    def _field_name(self, index):
        log(lambda: "KonanArraySyntheticProvider::_field_name({:#x}, {})".format(self._valobj.unsigned, index))
        return str(index)

    def to_short_string(self):
        log(lambda: "to_short_string:{:#x}".format(self._valobj.unsigned))
        return super().to_string(lambda index: "...")

    def to_string(self):
        log(lambda: "to_string:{self._valobj.unsigned:#x}")
        return super().to_string(lambda index: "{}".format(self._deref_or_obj_summary(index)))

class KonanZerroSyntheticProvider(lldb.SBSyntheticValueProvider):
    def __init__(self, valobj):
        log(lambda: "KonanZerroSyntheticProvider::__init__ {}".format(valobj.name))


    def num_children(self):
        log(lambda: "KonanZerroSyntheticProvider::num_children")
        return 0

    def has_children(self):
        log(lambda: "KonanZerroSyntheticProvider::has_children")
        return False

    def get_child_index(self, name):
        log(lambda: "KonanZerroSyntheticProvider::get_child_index")
        return 0

    def get_child_at_index(self, index):
        log(lambda: "KonanZerroSyntheticProvider::get_child_at_index")
        return None

    def to_string(self):
        log(lambda: "KonanZerroSyntheticProvider::to_string")
        return NULL

    def to_short_string(self):
        log(lambda: "KonanZerroSyntheticProvider::to_short_string")
        return NULL

    def __getattr__(self, item):
        pass

class KonanNullSyntheticProvider(KonanZerroSyntheticProvider):
    def __init__(self, valobj):
        super(KonanNullSyntheticProvider, self).__init__(valobj)

class KonanNotInitializedObjectSyntheticProvider(KonanZerroSyntheticProvider):
    def __init__(self, valobj):
        super(KonanNotInitializedObjectSyntheticProvider, self).__init__(valobj)


class KonanProxyTypeProvider:
    def __init__(self, valobj, internal_dict):
        start = time.monotonic()
        log(lambda : "KonanProxyTypeProvider:{:#x}, name: {}".format(valobj.unsigned, valobj.name))
        if valobj.unsigned == 0:
           log(lambda : "KonanProxyTypeProvider:{:#x}, name: {} NULL syntectic {}".format(valobj.unsigned, valobj.name,
                                                                                          valobj.IsValid()))
           bench(start, lambda: "KonanProxyTypeProvider({:#x})".format(valobj.unsigned))
           self._proxy = KonanNullSyntheticProvider(valobj)
           return

        tip = type_info(valobj)
        if not tip:
           log(lambda : "KonanProxyTypeProvider:{:#x}, name: {} not initialized syntectic {}".format(valobj.unsigned,
                                                                                                     valobj.name,
                                                                                                     valobj.IsValid()))
           bench(start, lambda: "KonanProxyTypeProvider({:#x})".format(valobj.unsigned))
           self._proxy = KonanNotInitializedObjectSyntheticProvider(valobj)
           return
        log(lambda : "KonanProxyTypeProvider:{:#x} tip: {:#x}".format(valobj.unsigned, tip))
        self._proxy = select_provider(valobj, tip, internal_dict)
        bench(start, lambda: "KonanProxyTypeProvider({:#x})".format(valobj.unsigned))
        log(lambda: "KonanProxyTypeProvider:{:#x} _proxy: {}".format(valobj.unsigned, self._proxy.__class__.__name__))

    def __del__(self):
        log(lambda: "KonanProxyTypeProvider del")

    def __getattr__(self, item):
       return getattr(self._proxy, item)


def type_name_command(debugger, command, result, internal_dict):
    log(lambda: "type_name_command")
    result.AppendMessage(evaluate('(char *)Konan_DebugGetTypeName({})'.format(command)).summary)


def _read_string(expr, error):
    if not error.Success():
        raise DebuggerException()
    log(lambda: "_read_string_n: will evaluate. expr:{}".format(expr))
    address = get_value_from_cached_dict(CachedDictType.READ_STRING,expr)
    if address is None:
        address = evaluate(expr).unsigned
        add_to_cached_dict(CachedDictType.READ_STRING,expr,address)
        log(lambda: "_read_string_n: get address from evaluate")
    else:
        log(lambda: "_read_string_n: get address from cache")
    log(lambda: "_read_string_n: after evaluate. expr:{} address:{:#x}".format(expr, address))
    return lldb.debugger.GetSelectedTarget().GetProcess().ReadCStringFromMemory(address, 0x1000, error)

__KONAN_VARIABLE = re.compile('kvar:(.*)#internal')
__KONAN_VARIABLE_TYPE = re.compile('^kfun:<get-(.*)>\\(\\)(.*)$')
__TYPES_KONAN_TO_C = {
   'kotlin.Byte': ('int8_t', lambda v: v.signed),
   'kotlin.Short': ('short', lambda v: v.signed),
   'kotlin.Int': ('int', lambda v: v.signed),
   'kotlin.Long': ('long', lambda v: v.signed),
   'kotlin.UByte': ('int8_t', lambda v: v.unsigned),
   'kotlin.UShort': ('short', lambda v: v.unsigned),
   'kotlin.UInt': ('int', lambda v: v.unsigned),
   'kotlin.ULong': ('long', lambda v: v.unsigned),
   'kotlin.Char': ('short', lambda v: v.signed),
   'kotlin.Boolean': ('bool', lambda v: v.signed),
   'kotlin.Float': ('float', lambda v: v.value),
   'kotlin.Double': ('double', lambda v: v.value)
}


def type_by_address_command(debugger, command, result, internal_dict):
    log(lambda: "type_by_address_command")
    result.AppendMessage("DEBUG: {}".format(command))
    tokens = command.split()
    target = debugger.GetSelectedTarget()
    types = _type_info_by_address(tokens[0])
    result.AppendMessage("DEBUG: {}".format(types))
    for t in types:
        result.AppendMessage("{}: {:#x}".format(t.name, t.GetStartAddress().GetLoadAddress(target)))


def symbol_by_name_command(debugger, command, result, internal_dict):
    log(lambda: "symbol_by_name_command")
    target = debugger.GetSelectedTarget()
    log(lambda : "symbol_by_name_command get target end")
    frame = target.GetProcess().GetSelectedThread().GetSelectedFrame()
    log(lambda : "symbol_by_name_command get frame end")
    tokens = command.split()
    mask = re.compile(tokens[0])
    symbols = list(filter(lambda v: mask.match(v.name), frame.GetModule().symbols))
    visited = list()
    for symbol in symbols:
       name = symbol.name
       if name in visited:
           continue
       visited.append(name)
       result.AppendMessage("{}: {:#x}".format(name, symbol.GetStartAddress().GetLoadAddress(target)))


def konan_globals_command(debugger, command, result, internal_dict):
    log(lambda: "konan_globals_command")
    target = debugger.GetSelectedTarget()
    log(lambda : "konan_globals_command get target end")
    frame = target.GetProcess().GetSelectedThread().GetSelectedFrame()
    log(lambda : "konan_globals_command get frame end")
    konan_variable_symbols = list(filter(lambda v: __KONAN_VARIABLE.match(v.name), frame.GetModule().symbols))
    visited = list()
    for symbol in konan_variable_symbols:
       name = __KONAN_VARIABLE.search(symbol.name).group(1)

       if name in visited:
           continue
       visited.append(name)

       getters = list(filter(lambda v: re.match('^kfun:<get-{}>\\(\\).*$'.format(name), v.name), frame.module.symbols))
       if not getters:
           result.AppendMessage("storage not found for name:{}".format(name))
           continue

       getter_functions = frame.module.FindFunctions(getters[0].name)
       if not getter_functions:
           continue

       address = getter_functions[0].function.GetStartAddress().GetLoadAddress(target)
       type = __KONAN_VARIABLE_TYPE.search(getters[0].name).group(2)
       (c_type, extractor) = __TYPES_KONAN_TO_C[type] if type in __TYPES_KONAN_TO_C.keys() else ('ObjHeader *', lambda v: kotlin_object_type_summary(v))
       value = evaluate('(({0} (*)()){1:#x})()'.format(c_type, address))
       str_value = extractor(value)
       result.AppendMessage('{} {}: {}'.format(type, name, str_value))

g_read_string_dict = {}
g_loaded_address_dict = {}
g_type_info_expr_dict = {}

def add_to_cached_dict(type: CachedDictType, key, value):
    if type is CachedDictType.READ_STRING:
        g_read_string_dict[key] = value
    elif type is CachedDictType.LOADED_ADDRESS:
        g_loaded_address_dict[key] = value
    elif type is CachedDictType.TYPE_INFO_EXPR:
        g_type_info_expr_dict[key] = value
    else:
        raise DebuggerException()


def clean_cached_dict(type: CachedDictType):
    if type is CachedDictType.READ_STRING:
        global g_read_string_dict
        g_read_string_dict = {}
    else:
        raise DebuggerException()

def get_value_from_cached_dict(type: CachedDictType, key):
    # 如果键不存在，返回None
    if type is CachedDictType.READ_STRING:
        return g_read_string_dict.get(key, None)
    elif type is CachedDictType.LOADED_ADDRESS:
        return g_loaded_address_dict.get(key, None)
    elif type is CachedDictType.TYPE_INFO_EXPR:
        return g_type_info_expr_dict.get(key, None)
    else:
        raise DebuggerException()

def __lldb_init_module(debugger, _):
    if exe_logging:
        file_path = os.getenv('HOME', '') + "/konan_lldb_log.txt"
        if os.path.exists(file_path):
            os.remove(file_path)

        global f
        if f is None or f.closed:
            f = open(file_path, "a")
            # close暂不处理

    log(lambda: "init start")
    __FACTORY['object'] = lambda x, y, z, a, b, c: KonanObjectSyntheticProvider(x, y, z, a, b, c)
    __FACTORY['array'] = lambda x, y, z: KonanArraySyntheticProvider(x, y, z)
    __FACTORY['string'] = lambda x, y, z: KonanStringSyntheticProvider(x, y, z)
    debugger.HandleCommand('\
        type summary add \
        --no-value \
        --expand \
        --python-function konan_lldb.kotlin_object_type_summary \
        "ObjHeader *" \
        --category Kotlin\
    ')
    debugger.HandleCommand('\
        type synthetic add \
        --python-class konan_lldb.KonanProxyTypeProvider \
        "ObjHeader *" \
        --category Kotlin\
    ')
    debugger.HandleCommand('type category enable Kotlin')
    debugger.HandleCommand('command script add -f {}.type_name_command type_name'.format(__name__))
    debugger.HandleCommand('command script add -f {}.type_by_address_command type_by_address'.format(__name__))
    debugger.HandleCommand('command script add -f {}.symbol_by_name_command symbol_by_name'.format(__name__))
    log(lambda: "init end")
