<template>
  <div class="relation-graph-wrap">
    <div ref="containerRef" class="relation-graph-container" />
    <div v-if="loading" class="relation-graph-loading">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>加载血缘图...</span>
    </div>
    <div v-else-if="empty" class="relation-graph-empty">
      <p>暂无白名单表（NORMAL/PREFERRED），请在表管理中配置可见性</p>
    </div>
    <div class="relation-graph-toolbar">
      <el-button
        v-if="!isAddingRelation"
        size="small"
        type="primary"
        icon="Plus"
        @click="enterAddMode"
        v-hasPermi="['metadata:relation:add']"
      >添加关系</el-button>
      <el-button v-else size="small" @click="exitAddMode">取消添加</el-button>
      <el-button size="small" icon="Refresh" @click="load">刷新</el-button>
    </div>
    <div v-if="isAddingRelation" class="relation-graph-hint">
      从源表拖拽到目标表以创建连线
    </div>

    <!-- 连线右键菜单 -->
    <div
      v-show="edgeContextMenu.visible"
      class="relation-graph-edge-menu"
      :style="{ left: edgeContextMenu.x + 'px', top: edgeContextMenu.y + 'px' }"
      @click.stop
    >
      <div class="edge-menu-item" @click="handleEdgeMenuEdit">
        <el-icon><Edit /></el-icon>
        <span>编辑</span>
      </div>
      <div class="edge-menu-item edge-menu-item-danger" @click="handleEdgeMenuDelete">
        <el-icon><Delete /></el-icon>
        <span>删除</span>
      </div>
    </div>

    <!-- 关系表单弹窗（新增/编辑） -->
    <el-dialog :title="relationFormTitle" v-model="relationFormVisible" width="520px" destroy-on-close @close="relationFormVisible = false">
      <el-form ref="relationFormRef" :model="relationForm" :rules="relationFormRules" label-width="100px">
        <el-form-item label="左表" prop="leftTable">
          <el-select v-model="relationForm.leftTable" placeholder="左表" filterable style="width:100%" @change="relationForm.leftField = ''">
            <el-option v-for="t in tables" :key="t.tableName" :label="t.tableComment ? `${t.tableName} (${t.tableComment})` : t.tableName" :value="t.tableName" />
          </el-select>
        </el-form-item>
        <el-form-item label="左表字段" prop="leftField">
          <el-select v-model="relationForm.leftField" placeholder="请选择左表字段" filterable style="width:100%" :loading="leftFieldLoading">
            <el-option v-for="f in leftFieldOptions" :key="f.fieldName" :label="f.fieldComment ? `${f.fieldName} (${f.fieldComment})` : f.fieldName" :value="f.fieldName" />
          </el-select>
        </el-form-item>
        <el-form-item label="右表" prop="rightTable">
          <el-select v-model="relationForm.rightTable" placeholder="右表" filterable style="width:100%" @change="relationForm.rightField = ''">
            <el-option v-for="t in tables" :key="t.tableName" :label="t.tableComment ? `${t.tableName} (${t.tableComment})` : t.tableName" :value="t.tableName" />
          </el-select>
        </el-form-item>
        <el-form-item label="右表字段" prop="rightField">
          <el-select v-model="relationForm.rightField" placeholder="请选择右表字段" filterable style="width:100%" :loading="rightFieldLoading">
            <el-option v-for="f in rightFieldOptions" :key="f.fieldName" :label="f.fieldComment ? `${f.fieldName} (${f.fieldComment})` : f.fieldName" :value="f.fieldName" />
          </el-select>
        </el-form-item>
        <el-form-item label="关系类型" prop="relationType">
          <el-select v-model="relationForm.relationType" placeholder="可选" clearable style="width:100%">
            <el-option label="INNER JOIN" value="INNER_JOIN" />
            <el-option label="LEFT JOIN" value="LEFT_JOIN" />
            <el-option label="RIGHT JOIN" value="RIGHT_JOIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-input-number v-model="relationForm.priority" :min="0" :max="100" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="relationForm.remark" type="textarea" :rows="2" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="relationFormVisible = false">取消</el-button>
        <el-button v-if="relationForm.id" type="danger" @click="handleDeleteFromForm">删除</el-button>
        <el-button type="primary" @click="submitRelationForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { Graph, NodeEvent, CommonEvent, EdgeEvent } from '@antv/g6'
import { getCurrentInstance } from 'vue'
import { getRelationGraphData, addTableRelation, updateTableRelation, delTableRelation, listFieldMetadataByTable } from '@/api/metadata'
import { Loading, Edit, Delete } from '@element-plus/icons-vue'

defineExpose({
  load,
  enterAddMode,
  exitAddMode
})

const { proxy } = getCurrentInstance()
const emit = defineEmits(['refreshed'])
const containerRef = ref(null)
const relationFormRef = ref(null)
const loading = ref(true)
const empty = ref(false)
const isAddingRelation = ref(false)
const tables = ref([])
const relations = ref([])
let graphInstance = null

const relationFormVisible = ref(false)
const relationFormTitle = ref('')
const relationForm = reactive({
  id: undefined,
  leftTable: '',
  leftField: '',
  rightTable: '',
  rightField: '',
  relationType: undefined,
  priority: 0,
  remark: ''
})
const relationFormRules = {
  leftTable: [{ required: true, message: '请选择左表', trigger: 'change' }],
  leftField: [{ required: true, message: '请选择左表字段', trigger: 'change' }],
  rightTable: [{ required: true, message: '请选择右表', trigger: 'change' }],
  rightField: [{ required: true, message: '请选择右表字段', trigger: 'change' }]
}

const leftFieldOptions = ref([])
const rightFieldOptions = ref([])
const leftFieldLoading = ref(false)
const rightFieldLoading = ref(false)

function getTableIdByTableName(tableName) {
  const t = tables.value.find(tb => (tb.tableName || '').trim() === (tableName || '').trim())
  return t?.id
}

function loadLeftFieldOptions() {
  const tableId = getTableIdByTableName(relationForm.leftTable)
  if (!tableId) {
    leftFieldOptions.value = []
    return Promise.resolve()
  }
  leftFieldLoading.value = true
  return listFieldMetadataByTable(tableId).then(res => {
    leftFieldOptions.value = res.data || []
    leftFieldLoading.value = false
  }).catch(() => {
    leftFieldLoading.value = false
  })
}

function loadRightFieldOptions() {
  const tableId = getTableIdByTableName(relationForm.rightTable)
  if (!tableId) {
    rightFieldOptions.value = []
    return Promise.resolve()
  }
  rightFieldLoading.value = true
  return listFieldMetadataByTable(tableId).then(res => {
    rightFieldOptions.value = res.data || []
    rightFieldLoading.value = false
  }).catch(() => {
    rightFieldLoading.value = false
  })
}

watch(() => relationForm.leftTable, () => {
  if (!isOpeningEdit.value) relationForm.leftField = ''
  loadLeftFieldOptions()
})

watch(() => relationForm.rightTable, () => {
  if (!isOpeningEdit.value) relationForm.rightField = ''
  loadRightFieldOptions()
})

const isOpeningEdit = ref(false)

watch(relationFormVisible, (visible) => {
  if (visible) {
    loadLeftFieldOptions()
    loadRightFieldOptions()
  }
})

function getEdgeArrowStyle(relationType) {
  switch (relationType) {
    case 'LEFT_JOIN':
      return { startArrow: false, endArrow: true, endArrowType: 'vee' }
    case 'RIGHT_JOIN':
      return { startArrow: true, endArrow: false, startArrowType: 'vee' }
    case 'INNER_JOIN':
      return { startArrow: true, endArrow: true, startArrowType: 'vee', endArrowType: 'vee' }
    default:
      return { startArrow: false, endArrow: false }
  }
}

function getRelationField(relation, side) {
  const key = side === 'left' ? 'leftField' : 'rightField'
  const keySnake = side === 'left' ? 'left_field' : 'right_field'
  return relation?.[key] ?? relation?.[keySnake] ?? ''
}

function getEdgeLabelText(relation) {
  const left = getRelationField(relation, 'left')
  const right = getRelationField(relation, 'right')
  switch (relation.relationType) {
    case 'LEFT_JOIN':
      return `${left} → ${right}`
    case 'RIGHT_JOIN':
      return `${left} ← ${right}`
    case 'INNER_JOIN':
      return `${left} ↔ ${right}`
    default:
      return `${left} — ${right}`
  }
}

function buildGraphData() {
  const nodeMap = new Map()
  tables.value.forEach(t => {
    const name = (t.tableName || '').trim()
    if (name) nodeMap.set(name, t)
  })
  const nodes = tables.value
    .filter(t => (t.tableName || '').trim())
    .map(t => {
      const comment = t.tableComment || t.tableName || ''
      const physical = t.tableName || ''
      return {
        id: t.tableName.trim(),
        data: { table: t },
        style: {
          size: [160, 56],
          fill: '#1e293b',
          stroke: '#14b8a6',
          lineWidth: 1.5,
          radius: 6,
          labelText: comment && comment !== physical ? `${comment} (${physical})` : (comment || physical),
          labelFill: '#f0fdfa',
          labelFontSize: 12,
          labelFontWeight: 500,
          labelPlacement: 'center'
        }
      }
    })
  const edgeList = []
  relations.value.forEach((r, i) => {
    if (nodeMap.has(r.leftTable) && nodeMap.has(r.rightTable)) {
      const arrowStyle = getEdgeArrowStyle(r.relationType)
      edgeList.push({
        id: `e-${r.id || i}`,
        source: r.leftTable,
        target: r.rightTable,
        data: { relation: r },
        style: {
          stroke: '#94a3b8',
          lineWidth: 1.5,
          labelText: getEdgeLabelText(r),
          labelFill: '#f0fdfa',
          labelFontSize: 11,
          labelBackground: true,
          labelBackgroundFill: '#1e293b',
          labelBackgroundRadius: 4,
          ...arrowStyle
        }
      })
    }
  })
  return { nodes, edges: edgeList }
}

const ASSIST_NODE_ID = 'relation-graph-assist-node'
const ASSIST_EDGE_ID = 'relation-graph-assist-edge'

function getBehaviors() {
  const base = [
    { type: 'drag-canvas' },
    { type: 'zoom-canvas' }
  ]
  if (!isAddingRelation.value) {
    base.push({ type: 'drag-element' })
  }
  return base
}

function bindCreateEdgeHandlers() {
  if (!graphInstance) return
  creatingSource.value = null
  graphInstance.on(NodeEvent.POINTER_DOWN, onNodePointerDown)
  graphInstance.on(CommonEvent.POINTER_MOVE, onPointerMove)
  graphInstance.on(CommonEvent.POINTER_UP, onPointerUp)
  window.addEventListener('pointerup', onWindowPointerUp)
}

function unbindCreateEdgeHandlers() {
  window.removeEventListener('pointerup', onWindowPointerUp)
  if (!graphInstance) return
  graphInstance.off(NodeEvent.POINTER_DOWN, onNodePointerDown)
  graphInstance.off(CommonEvent.POINTER_MOVE, onPointerMove)
  graphInstance.off(CommonEvent.POINTER_UP, onPointerUp)
  cancelCreateEdge()
}

function onWindowPointerUp() {
  if (creatingSource.value && graphInstance) {
    cancelCreateEdge()
  }
}

const creatingSource = ref(null)

const edgeContextMenu = reactive({
  visible: false,
  x: 0,
  y: 0,
  relation: null
})

async function onNodePointerDown(ev) {
  const nodeId = ev.target?.id
  if (!nodeId || !tables.value.some(t => t.tableName === nodeId)) return
  creatingSource.value = nodeId
  const sourceNode = graphInstance.getElementData(nodeId)
  const pos = sourceNode?.style ? [sourceNode.style.x ?? 0, sourceNode.style.y ?? 0] : [0, 0]
  graphInstance.addNodeData([{
    id: ASSIST_NODE_ID,
    type: 'circle',
    style: {
      size: 1,
      visibility: 'hidden',
      x: pos[0],
      y: pos[1],
      ports: [{ key: 'port-1', placement: [0.5, 0.5] }]
    }
  }])
  graphInstance.addEdgeData([{
    id: ASSIST_EDGE_ID,
    source: nodeId,
    target: ASSIST_NODE_ID,
    style: {
      stroke: '#14b8a6',
      lineWidth: 2,
      pointerEvents: 'none'
    }
  }])
  graphInstance.context?.canvas?.setCursor?.('crosshair')
  await graphInstance.context?.element?.draw?.({ animation: false })?.finished
}

async function onPointerMove(ev) {
  if (!creatingSource.value || !graphInstance) return
  const x = ev.client?.x ?? ev.clientX ?? 0
  const y = ev.client?.y ?? ev.clientY ?? 0
  const canvasPoint = graphInstance.getCanvasByClient([x, y])
  graphInstance.updateNodeData([{
    id: ASSIST_NODE_ID,
    style: { x: canvasPoint[0], y: canvasPoint[1] }
  }])
  await graphInstance.context?.element?.draw?.({ animation: false, silence: true })?.finished
}

async function onPointerUp(ev) {
  if (!creatingSource.value || !graphInstance) return
  const targetType = ev.targetType ?? ev.target?.targetType
  const targetId = ev.target?.id
  const isOnNode = targetType === 'node' || (targetId && tables.value.some(t => t.tableName === targetId))
  if (isOnNode && targetId && targetId !== creatingSource.value && targetId !== ASSIST_NODE_ID) {
    const left = creatingSource.value
    const right = targetId
    await cancelCreateEdge()
    openFormFromConnection(left, right)
  } else {
    await cancelCreateEdge()
  }
}

async function cancelCreateEdge() {
  if (!graphInstance) return
  if (creatingSource.value) {
    graphInstance.removeEdgeData([ASSIST_EDGE_ID])
    graphInstance.removeNodeData([ASSIST_NODE_ID])
  }
  creatingSource.value = null
  graphInstance.context?.canvas?.setCursor?.('default')
  await graphInstance.context?.element?.draw?.({ animation: false })?.finished
}

function openFormFromConnection(leftTable, rightTable) {
  relationForm.id = undefined
  relationForm.leftTable = leftTable
  relationForm.rightTable = rightTable
  relationForm.leftField = ''
  relationForm.rightField = ''
  relationForm.relationType = undefined
  relationForm.priority = 0
  relationForm.remark = ''
  relationFormTitle.value = '添加关系（请填写关联字段）'
  relationFormVisible.value = true
}

function enterAddMode() {
  if (empty.value || !graphInstance) return
  isAddingRelation.value = true
  graphInstance.setBehaviors(getBehaviors())
  bindCreateEdgeHandlers()
}

function exitAddMode() {
  unbindCreateEdgeHandlers()
  isAddingRelation.value = false
  if (graphInstance) {
    graphInstance.setBehaviors(getBehaviors())
  }
}

function initGraph() {
  if (!containerRef.value) return
  const { nodes, edges } = buildGraphData()
  if (nodes.length === 0) {
    empty.value = true
    return
  }
  empty.value = false
  graphInstance?.destroy()
  graphInstance = new Graph({
    container: containerRef.value,
    width: containerRef.value.offsetWidth,
    height: containerRef.value.offsetHeight,
    data: { nodes, edges },
    theme: 'dark',
    node: {
      type: 'rect',
      style: {
        size: [160, 56],
        fill: '#1e293b',
        stroke: '#14b8a6',
        lineWidth: 1.5,
        radius: 6,
        labelText: (d) => {
          const t = d.data?.table
          const comment = t?.tableComment || t?.tableName || ''
          const physical = t?.tableName || d.id || ''
          return comment && comment !== physical ? `${comment} (${physical})` : (comment || physical)
        },
        labelFill: '#f0fdfa',
        labelFontSize: 12,
        labelFontWeight: 500,
        labelPlacement: 'center'
      },
      state: {
        hover: { stroke: '#22d3ee', lineWidth: 2 },
        selected: { stroke: '#22d3ee', lineWidth: 2 }
      }
    },
    edge: {
      type: 'quadratic',
      style: {
        stroke: '#94a3b8',
        lineWidth: 1.5,
        labelText: (d) => {
          const r = d.data?.relation
          return r ? getEdgeLabelText(r) : ''
        },
        labelFill: '#f0fdfa',
        labelFontSize: 11
      },
      state: {
        hover: { stroke: '#5eead4', lineWidth: 2 },
        selected: { stroke: '#5eead4', lineWidth: 2 }
      }
    },
    layout: {
      type: 'force',
      preventOverlap: true,
      nodeSpacing: 80,
      linkDistance: 180
    },
    behaviors: getBehaviors()
  })

  graphInstance.on('edge:click', (ev) => {
    const target = ev.target
    const id = target?.id
    if (!id || typeof id !== 'string') return
    const relationId = id.startsWith('e-') ? id.slice(2) : null
    const rel = relations.value.find(r => String(r.id) === relationId || id === `e-${r.id}`)
    if (rel) openEditForm(rel)
  })

  graphInstance.on(EdgeEvent.CONTEXT_MENU, (ev) => {
    ev.preventDefault?.()
    ev.stopPropagation?.()
    const target = ev.target
    const id = target?.id
    if (!id || typeof id !== 'string') return
    const relationId = id.startsWith('e-') ? id.slice(2) : null
    const rel = relations.value.find(r => String(r.id) === relationId || id === `e-${r.id}`)
    if (rel) {
      const clientX = ev.client?.x ?? ev.clientX ?? 0
      const clientY = ev.client?.y ?? ev.clientY ?? 0
      edgeContextMenu.relation = rel
      edgeContextMenu.x = clientX
      edgeContextMenu.y = clientY
      edgeContextMenu.visible = true
    }
  })

  document.addEventListener('click', hideEdgeContextMenu)
  document.addEventListener('contextmenu', hideEdgeContextMenu)

  graphInstance.render()
  graphInstance.fitView?.({ padding: 40 })
  const container = graphInstance.context?.canvas?.getContainer?.()
  if (container) {
    container.addEventListener('contextmenu', (e) => e.preventDefault())
  }
  if (isAddingRelation.value) {
    bindCreateEdgeHandlers()
  }
}

async function openEditForm(relation) {
  isOpeningEdit.value = true
  relationForm.id = relation.id
  relationForm.leftTable = relation.leftTable ?? relation.left_table ?? ''
  relationForm.leftField = getRelationField(relation, 'left')
  relationForm.rightTable = relation.rightTable ?? relation.right_table ?? ''
  relationForm.rightField = getRelationField(relation, 'right')
  relationForm.relationType = relation.relationType
  relationForm.priority = relation.priority ?? 0
  relationForm.remark = relation.remark || ''
  relationFormTitle.value = '编辑表关系'
  await Promise.all([loadLeftFieldOptions(), loadRightFieldOptions()])
  relationFormVisible.value = true
  nextTick(() => {
    isOpeningEdit.value = false
  })
}

function submitRelationForm() {
  relationFormRef.value?.validate(valid => {
    if (!valid) return
    const payload = {
      leftTable: relationForm.leftTable,
      leftField: relationForm.leftField,
      rightTable: relationForm.rightTable,
      rightField: relationForm.rightField,
      relationType: relationForm.relationType,
      priority: relationForm.priority,
      remark: relationForm.remark
    }
    if (relationForm.id) {
      updateTableRelation(relationForm.id, payload).then(() => {
        proxy.$modal.msgSuccess('修改成功')
        relationFormVisible.value = false
        load()
      }).catch(() => {})
    } else {
      addTableRelation(payload).then(() => {
        proxy.$modal.msgSuccess('新增成功')
        relationFormVisible.value = false
        load()
      }).catch(() => {})
    }
  })
}

function hideEdgeContextMenu() {
  edgeContextMenu.visible = false
  edgeContextMenu.relation = null
}

function handleEdgeMenuEdit() {
  if (edgeContextMenu.relation) {
    openEditForm(edgeContextMenu.relation)
  }
  hideEdgeContextMenu()
}

function handleEdgeMenuDelete() {
  const rel = edgeContextMenu.relation
  hideEdgeContextMenu()
  if (!rel?.id) return
  proxy.$modal.confirm('确认删除该表关系？').then(() => {
    return delTableRelation([rel.id])
  }).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    load()
  })
}

function handleDeleteFromForm() {
  proxy.$modal.confirm('确认删除该表关系？').then(() => {
    return delTableRelation([relationForm.id])
  }).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    relationFormVisible.value = false
    load()
  })
}

function load() {
  loading.value = true
  getRelationGraphData().then(res => {
    tables.value = res.data?.tables || []
    relations.value = res.data?.relations || []
    loading.value = false
    initGraph()
    emit('refreshed')
  }).catch(() => {
    loading.value = false
  })
}

function resize() {
  if (graphInstance && containerRef.value) {
    graphInstance.setSize(containerRef.value.offsetWidth, containerRef.value.offsetHeight)
  }
}

onMounted(() => {
  load()
  window.addEventListener('resize', resize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  document.removeEventListener('click', hideEdgeContextMenu)
  document.removeEventListener('contextmenu', hideEdgeContextMenu)
  graphInstance?.destroy()
  graphInstance = null
})
</script>

<style scoped>
.relation-graph-wrap {
  position: relative;
  width: 100%;
  flex: 1;
  min-height: 400px;
  background: linear-gradient(145deg, #0f172a 0%, #1e293b 50%, #0f172a 100%);
  border-radius: 8px;
  border: 1px solid rgba(20, 184, 166, 0.15);
  overflow: hidden;
}

.relation-graph-container {
  width: 100%;
  height: 100%;
  min-height: 400px;
}

.relation-graph-loading,
.relation-graph-empty {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #94a3b8;
  font-size: 14px;
  font-family: 'PingFang SC', 'Microsoft YaHei', 'Helvetica Neue', Helvetica, Arial, sans-serif;
  background: rgba(15, 23, 42, 0.6);
}

.relation-graph-loading .el-icon {
  font-size: 28px;
  color: #14b8a6;
}

.relation-graph-empty p {
  margin: 0;
  max-width: 320px;
  text-align: center;
  line-height: 1.6;
}

.relation-graph-toolbar {
  position: absolute;
  bottom: 12px;
  right: 12px;
  display: flex;
  gap: 8px;
}

.relation-graph-hint {
  position: absolute;
  top: 12px;
  left: 50%;
  transform: translateX(-50%);
  padding: 6px 16px;
  background: rgba(20, 184, 166, 0.15);
  border: 1px solid rgba(20, 184, 166, 0.4);
  border-radius: 6px;
  color: #5eead4;
  font-size: 13px;
}

.relation-graph-edge-menu {
  position: fixed;
  z-index: 9999;
  background: #1e293b;
  border: 1px solid rgba(20, 184, 166, 0.4);
  border-radius: 6px;
  padding: 4px 0;
  min-width: 100px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4);
}

.relation-graph-edge-menu .edge-menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  cursor: pointer;
  color: #e2e8f0;
  font-size: 13px;
  font-family: 'PingFang SC', 'Microsoft YaHei', 'Helvetica Neue', Helvetica, Arial, sans-serif;
}

.relation-graph-edge-menu .edge-menu-item:hover {
  background: rgba(20, 184, 166, 0.15);
  color: #5eead4;
}

.relation-graph-edge-menu .edge-menu-item-danger:hover {
  background: rgba(239, 68, 68, 0.2);
  color: #f87171;
}
</style>
