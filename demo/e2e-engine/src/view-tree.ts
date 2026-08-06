import type { UiTreeNode, UiViewTree, Platform } from "./mobile-driver.js"

interface RawXmlNode {
  tag: string
  attrs: Record<string, string>
  children: RawXmlNode[]
}

export function parseXmlToTree(xml: string): RawXmlNode | null {
  const stack: RawXmlNode[] = []
  let root: RawXmlNode | null = null

  const tokens = xml.split(/>\s*</)
  for (const token of tokens) {
    const trimmed = token.trim().replace(/^</, "")
    if (!trimmed) continue

    if (trimmed.startsWith("?") || trimmed.startsWith("!")) continue

    const isClosing = trimmed.startsWith("/")
    if (isClosing) {
      stack.pop()
      continue
    }

    const cleanToken = trimmed.replace(/\/$/, "").replace(/>$/, "").trim()
    if (!cleanToken) continue

    const { tagName, attrs } = parseTag(cleanToken)
    if (!tagName) continue

    const isSelfClosing = trimmed.endsWith("/")

    const node: RawXmlNode = { tag: tagName, attrs, children: [] }

    if (stack.length > 0) {
      stack[stack.length - 1].children.push(node)
    } else if (!root) {
      root = node
    }

    if (!isSelfClosing) {
      stack.push(node)
    }
  }

  return root
}

function parseTag(token: string): { tagName: string; attrs: Record<string, string> } {
  const spaceIdx = token.indexOf(" ")
  if (spaceIdx === -1) {
    return { tagName: token, attrs: {} }
  }
  const tagName = token.substring(0, spaceIdx)
  const attrStr = token.substring(spaceIdx + 1)
  const attrs: Record<string, string> = {}

  const regex = /([\w-]+)="([^"]*)"/g
  let match
  while ((match = regex.exec(attrStr)) !== null) {
    attrs[match[1]] = match[2]
  }

  return { tagName, attrs }
}

function simplifyType(rawType: string): string {
  return rawType
    .replace(/^XCUIElementType/, "")
    .replace(/^android\.widget\./, "")
    .replace(/^android\.view\./, "")
    .replace(/^androidx\.recyclerview\.widget\./, "")
}

function rawNodeToTreeNode(raw: RawXmlNode, platform: Platform, parentBounds?: [number, number, number, number]): UiTreeNode {
  const a = raw.attrs
  const rawType = a.type || a.class || raw.tag

  let debugName = ""
  let testTag = ""
  let accessibilityText = ""

  if (platform === "android") {
    const contentDesc = a["content-desc"] || ""
    const resourceId = a["resource-id"] || ""
    const extractedTag = extractTestTag(resourceId)
    if (extractedTag) {
      testTag = extractedTag
    }
    if (looksLikeClassName(contentDesc)) {
      debugName = contentDesc
    }
    if (contentDesc && !looksLikeClassName(contentDesc) && contentDesc !== testTag) {
      accessibilityText = contentDesc
    }
  } else {
    const name = a.name || ""
    const label = a.label || ""
    if (name && a.type?.startsWith("XCUIElementType") && name !== label) {
      const spaceIdx = name.indexOf(" ")
      if (spaceIdx !== -1) {
        debugName = name.substring(0, spaceIdx)
        testTag = name.substring(spaceIdx + 1)
      } else if (looksLikeClassName(name)) {
        debugName = name
      } else {
        testTag = name
      }
    }
  }

  // Parse screen-absolute bounds first, so we can pass them to children
  let parsedBounds: [number, number, number, number] | undefined
  const boundsMatch = a.bounds?.match(/^\[([^\]]*)\]\[([^\]]*)\]$/)
  if (boundsMatch) {
    const [x1, y1] = boundsMatch[1].split(",").map(Number)
    const [x2, y2] = boundsMatch[2].split(",").map(Number)
    parsedBounds = [x1, y1, x2 - x1, y2 - y1]
  } else if (a.x !== undefined && a.y !== undefined && a.width !== undefined && a.height !== undefined) {
    const x = Number(a.x), y = Number(a.y), w = Number(a.width), h = Number(a.height)
    parsedBounds = [x, y, w, h]
  }

  const type = debugName || simplifyType(rawType)

  const node: UiTreeNode = {
    type,
    children: raw.children.map(c => rawNodeToTreeNode(c, platform, parsedBounds)),
  }

  if (rawType && rawType !== type) node.rawType = rawType
  if (testTag) node.testTag = testTag

  const name = a.name || ""
  const label = a.label || ""

  const rawText = a.text
  const rawValue = a.value
  const textVal = accessibilityText || rawText || rawValue || label || name
  if (textVal && textVal !== testTag && textVal !== debugName) node.text = textVal

  if (rawValue && rawValue !== textVal) node.value = rawValue
  if (a.placeholderValue) node.placeholder = a.placeholderValue

  if (a.accessible === "true") node.accessible = true
  if (a.enabled === "false") node.enabled = false
  if (a.visible === "false" || a.displayed === "false") node.visible = false
  if (a.clickable === "true") node.clickable = true
  if (a.checked === "true") node.checked = true
  if (a.scrollable === "true") node.scrollable = true
  if (a.traits && a.traits !== "") node.traits = a.traits

  if (parsedBounds) {
    node.bounds = parsedBounds
    if (parentBounds) {
      const [px, py] = parentBounds
      const [x, y, w, h] = parsedBounds
      node.boundsParent = [x - px, y - py, w, h]
    }
  }

  return node
}

function extractTestTag(resourceId: string): string {
  if (!resourceId) return ""
  const colonIdx = resourceId.indexOf(":id/")
  if (colonIdx !== -1) {
    return resourceId.substring(colonIdx + 4)
  }
  if (resourceId.includes(":")) return ""
  return resourceId
}

function looksLikeClassName(name: string): boolean {
  if (!name) return false
  const first = name[0]
  return first === first.toUpperCase() && /^[A-Z][A-Za-z0-9]+$/.test(name)
}

export function buildViewTree(xml: string, platform: Platform, viewport: [number, number]): UiViewTree | null {
  let raw = parseXmlToTree(xml)
  if (!raw) return null

  if (raw.tag === "AppiumAUT" && raw.children.length === 1) {
    raw = raw.children[0]
  }

  const tree = rawNodeToTreeNode(raw, platform)

  return {
    platform,
    source: "appium",
    viewport,
    tree,
  }
}

export function filterVisibleTree(node: UiTreeNode): UiTreeNode | null {
  if (node.visible === false) return null

  const isKeyboard = node.type === "Keyboard"
  if (isKeyboard) return null

  const isZeroSize = node.bounds && node.bounds[2] === 0 && node.bounds[3] === 0
  if (isZeroSize) return null

  const filteredChildren = node.children
    .map(child => filterVisibleTree(child))
    .filter((c): c is UiTreeNode => c !== null)

  return { ...node, children: filteredChildren }
}

export function renderTreeText(node: UiTreeNode, prefix: string = "", isLast: boolean = true, isRoot: boolean = true): string {
  const connector = isRoot ? "" : (isLast ? "└── " : "├── ")
  const childPrefix = isRoot ? "" : (isLast ? "    " : "│   ")

  const parts: string[] = [node.type]
  if (node.rawType) parts.push(`(${node.rawType})`)
  if (node.testTag) parts.push(`testTag="${node.testTag}"`)
  if (node.text) parts.push(`text="${node.text}"`)
  if (node.value && node.value !== node.text) parts.push(`value="${node.value}"`)
  if (node.placeholder) parts.push(`placeholder="${node.placeholder}"`)
  if (node.accessible) parts.push("accessible=true")
  if (node.enabled === false) parts.push("enabled=false")
  if (node.visible === false) parts.push("visible=false")
  if (node.clickable) parts.push("clickable=true")
  if (node.checked) parts.push("checked=true")
  if (node.scrollable) parts.push("scrollable=true")
  if (node.traits && node.traits !== "") parts.push(`traits="${node.traits}"`)
  if (node.bounds) parts.push(`screen:[${node.bounds[0]},${node.bounds[1]},${node.bounds[2]},${node.bounds[3]}]`)
  if (node.boundsParent) parts.push(`parent:[${node.boundsParent[0]},${node.boundsParent[1]},${node.boundsParent[2]},${node.boundsParent[3]}]`)

  const line = prefix + connector + parts.join(" ")

  const childLines = node.children.map((child, idx) => {
    const childIsLast = idx === node.children.length - 1
    return renderTreeText(child, prefix + childPrefix, childIsLast, false)
  })

  return [line, ...childLines].join("\n")
}

export function collapseSingleChildren(node: UiTreeNode): UiTreeNode {
  const collapsedChildren = node.children
    .map(c => collapseSingleChildren(c))
    .flatMap(c => {
      if (c.children.length === 1 && !c.testTag && !c.text && !c.value && !c.placeholder && c.accessible !== true && c.clickable !== true && c.scrollable !== true) {
        return [c.children[0]]
      }
      return [c]
    })

  return { ...node, children: collapsedChildren }
}
