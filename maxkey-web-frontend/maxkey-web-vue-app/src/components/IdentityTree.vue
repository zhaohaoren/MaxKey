<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { FileOutlined, FolderOpenOutlined, FolderOutlined } from '@ant-design/icons-vue'
import type { IdentityTreeNode } from '../identity'

const props = defineProps<{
  nodes: IdentityTreeNode[]
  selectedKey?: string
}>()

const emit = defineEmits<{
  select: [node: IdentityTreeNode]
}>()

const selectedKeys = computed(() => props.selectedKey ? [props.selectedKey] : [])
const expandedKeys = ref<string[]>([])

function collectExpanded(nodes: IdentityTreeNode[]): string[] {
  return nodes.flatMap(node => node.children?.length ? [node.key, ...collectExpanded(node.children)] : [])
}

watch(() => props.nodes, nodes => { expandedKeys.value = collectExpanded(nodes) }, { immediate: true })

function onSelect(_keys: Array<string | number>, info: { node: IdentityTreeNode }) {
  emit('select', info.node)
}

function onExpand(keys: Array<string | number>) {
  expandedKeys.value = keys.map(String)
}
</script>

<template>
  <a-tree
    block-node
    show-line
    default-expand-all
    :tree-data="nodes"
    :selected-keys="selectedKeys"
    :expanded-keys="expandedKeys"
    @expand="onExpand"
    @select="onSelect"
  >
    <template #title="node">
      <span class="identity-tree-node">
        <FileOutlined v-if="node.isLeaf" />
        <FolderOpenOutlined v-else-if="node.expanded" />
        <FolderOutlined v-else />
        <span>{{ node.title }}{{ node.type === 'virtual' ? '_v' : '' }}</span>
      </span>
    </template>
  </a-tree>
</template>

<style scoped>
.identity-tree-node { display: inline-flex; align-items: center; gap: 5px; }
</style>
