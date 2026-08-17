export interface IdentityRow {
  id?: string | number
  disabled?: boolean
  [key: string]: unknown
}

export interface IdentityPage {
  rows?: IdentityRow[]
  records?: number
  total?: number
}

export interface IdentityTreeNode {
  key: string
  value: string
  title: string
  type?: string
  parentKey?: string
  children?: IdentityTreeNode[]
  isLeaf?: boolean
}

interface LegacyTreeNode {
  key?: string
  title?: string
  parentKey?: string
  attrs?: { type?: string }
}

interface LegacyTree {
  rootNode?: LegacyTreeNode
  nodes?: LegacyTreeNode[]
}

export function normalizeIdentityPage(data: unknown) {
  if (Array.isArray(data)) return { rows: data as IdentityRow[], total: data.length }
  const value = (data || {}) as IdentityPage
  const rows = Array.isArray(value.rows) ? value.rows : []
  const total = typeof value.records === 'number' ? value.records : typeof value.total === 'number' ? value.total : rows.length
  return { rows, total }
}

export function isEnabled(value: unknown) {
  return value === true || value === 1 || value === '1' || value === 'true'
}

export function buildOrganizationTree(data: unknown): IdentityTreeNode[] {
  const tree = (data || {}) as LegacyTree
  if (!tree.rootNode?.key) return []
  const source = Array.isArray(tree.nodes) ? tree.nodes : []

  const build = (node: LegacyTreeNode): IdentityTreeNode => {
    const key = String(node.key || '')
    const children = source
      .filter(item => String(item.key || '') !== key && String(item.parentKey || '') === key)
      .map(build)
    return {
      key,
      value: key,
      title: String(node.title || ''),
      type: node.attrs?.type,
      parentKey: node.parentKey,
      children,
      isLeaf: children.length === 0,
    }
  }

  return [build(tree.rootNode)]
}

export function findTreeNode(nodes: IdentityTreeNode[], key: string): IdentityTreeNode | undefined {
  for (const node of nodes) {
    if (node.key === key) return node
    const child = findTreeNode(node.children || [], key)
    if (child) return child
  }
  return undefined
}
