<template>
  <div class="feedback-page">
    <div class="page-header">
      <div>
        <h2 class="page-heading">Feedback Issues</h2>
        <p class="page-copy">Review Sprint 4 customer feedback, prioritize safety issues, and record handling notes.</p>
      </div>
      <div class="header-actions">
        <el-button :loading="loading" @click="loadIssues">
          <el-icon><Refresh /></el-icon>
          Refresh
        </el-button>
        <el-button type="danger" plain :loading="loading" @click="loadHighPriority">
          High Priority
        </el-button>
      </div>
    </div>

    <el-row :gutter="20" class="stats-row">
      <el-col :xs="24" :sm="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">Total Issues</div>
          <div class="stat-value">{{ issues.length }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">High Priority</div>
          <div class="stat-value stat-danger">{{ highPriorityCount }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">Open / In Progress</div>
          <div class="stat-value">{{ activeIssueCount }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="filter-card">
      <el-row :gutter="16">
        <el-col :xs="24" :sm="12" :lg="5">
          <el-select v-model="filters.priority" clearable placeholder="Priority" style="width: 100%">
            <el-option label="LOW" value="LOW" />
            <el-option label="HIGH" value="HIGH" />
          </el-select>
        </el-col>
        <el-col :xs="24" :sm="12" :lg="6">
          <el-select v-model="filters.status" clearable placeholder="Status" style="width: 100%">
            <el-option label="OPEN" value="OPEN" />
            <el-option label="IN_PROGRESS" value="IN_PROGRESS" />
            <el-option label="RESOLVED" value="RESOLVED" />
          </el-select>
        </el-col>
        <el-col :xs="24" :sm="16" :lg="9">
          <el-input
            v-model="filters.keyword"
            maxlength="100"
            clearable
            placeholder="Search category or content"
            prefix-icon="Search"
            @keyup.enter="loadIssues"
          />
        </el-col>
        <el-col :xs="24" :sm="8" :lg="4">
          <el-button type="primary" class="filter-button" @click="loadIssues">
            <el-icon><Search /></el-icon>
            Search
          </el-button>
        </el-col>
      </el-row>
    </el-card>

    <el-card>
      <div class="table-scroll">
        <el-table :data="issues" stripe v-loading="loading">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column label="Issue" min-width="300">
            <template #default="{ row }">
              <div class="issue-cell">
                <span class="issue-title">{{ categoryLabel(row.category) }}</span>
                <span class="issue-content">{{ row.content }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="Customer" min-width="190">
            <template #default="{ row }">
              <div class="stack-cell">
                <span>{{ row.username || `User #${row.userId}` }}</span>
                <small>{{ row.userEmail || '-' }}</small>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="Booking" min-width="180">
            <template #default="{ row }">
              <div class="stack-cell">
                <span>#{{ row.bookingId }}</span>
                <small>{{ row.rentalType || '-' }} · {{ row.bookingStatus || '-' }}</small>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="Priority" width="130">
            <template #default="{ row }">
              <el-tag :type="row.priority === 'HIGH' ? 'danger' : 'info'" effect="plain">
                {{ row.priority }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="Status" width="150">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" effect="plain">
                {{ row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="Created" min-width="170">
            <template #default="{ row }">
              {{ formatDateTime(row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column label="Actions" width="130" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link @click="openEdit(row)">
                Handle
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div v-if="issues.length === 0 && !loading" class="empty-tip">
        <el-empty description="No feedback issues found." />
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      title="Handle Feedback Issue"
      width="min(620px, 94vw)"
      :close-on-click-modal="false"
    >
      <div v-if="editingIssue" class="dialog-summary">
        <div class="dialog-title">#{{ editingIssue.id }} · {{ categoryLabel(editingIssue.category) }}</div>
        <div class="dialog-copy">{{ editingIssue.content }}</div>
      </div>

      <el-form :model="editForm" label-position="top">
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12">
            <el-form-item label="Priority">
              <el-select v-model="editForm.priority" style="width: 100%">
                <el-option label="LOW" value="LOW" />
                <el-option label="HIGH" value="HIGH" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="Status">
              <el-select v-model="editForm.status" style="width: 100%">
                <el-option label="OPEN" value="OPEN" />
                <el-option label="IN_PROGRESS" value="IN_PROGRESS" />
                <el-option label="RESOLVED" value="RESOLVED" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="Resolution Note">
          <el-input
            v-model="editForm.resolutionNote"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="Add a handling note for the customer or support record."
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">Cancel</el-button>
        <el-button type="primary" :loading="submitting" @click="submitUpdate">
          Save Issue
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  listFeedbackIssues,
  listHighPriorityFeedbackIssues,
  updateFeedbackIssue,
  type FeedbackIssueDto
} from '@/api/admin'
import { formatDateTime } from '@/utils/admin-display'

const CATEGORY_LABELS: Record<string, string> = {
  SCOOTER_FAULT: 'Scooter Fault',
  BOOKING: 'Booking Issue',
  PAYMENT: 'Payment Issue',
  OTHER: 'Other'
}

const issues = ref<FeedbackIssueDto[]>([])
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const editingIssue = ref<FeedbackIssueDto | null>(null)

const filters = reactive({
  priority: '',
  status: '',
  keyword: ''
})

const editForm = reactive({
  priority: 'LOW',
  status: 'OPEN',
  resolutionNote: ''
})

const highPriorityCount = computed(() => issues.value.filter(issue => issue.priority === 'HIGH').length)
const activeIssueCount = computed(() => issues.value.filter(issue => issue.status === 'OPEN' || issue.status === 'IN_PROGRESS').length)

function buildFilterParams() {
  return {
    priority: filters.priority || undefined,
    status: filters.status || undefined,
    keyword: filters.keyword.trim() || undefined
  }
}

function categoryLabel(category: string) {
  return CATEGORY_LABELS[category] || category || '-'
}

function statusTagType(status: string) {
  if (status === 'RESOLVED') return 'success'
  if (status === 'IN_PROGRESS') return 'warning'
  return 'danger'
}

async function loadIssues() {
  loading.value = true
  try {
    const res = await listFeedbackIssues(buildFilterParams())
    issues.value = res.data || []
  } catch {
    issues.value = []
  } finally {
    loading.value = false
  }
}

async function loadHighPriority() {
  filters.priority = 'HIGH'
  filters.status = ''
  filters.keyword = ''
  loading.value = true
  try {
    const res = await listHighPriorityFeedbackIssues()
    issues.value = res.data || []
  } catch {
    issues.value = []
  } finally {
    loading.value = false
  }
}

function openEdit(issue: FeedbackIssueDto) {
  editingIssue.value = issue
  editForm.priority = issue.priority || 'LOW'
  editForm.status = issue.status || 'OPEN'
  editForm.resolutionNote = issue.resolutionNote || ''
  dialogVisible.value = true
}

async function submitUpdate() {
  if (!editingIssue.value) return
  if (editForm.resolutionNote.length > 500) {
    ElMessage.warning('Resolution note must be 500 characters or fewer.')
    return
  }

  submitting.value = true
  try {
    await updateFeedbackIssue(editingIssue.value.id, {
      priority: editForm.priority,
      status: editForm.status,
      resolutionNote: editForm.resolutionNote.trim()
    })
    ElMessage.success('Feedback issue updated successfully.')
    dialogVisible.value = false
    await loadIssues()
  } catch {
    // request interceptor handles backend messages
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadIssues()
})
</script>

<style scoped>
.feedback-page {
  max-width: 1400px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 20px;
}

.page-heading {
  font-size: 24px;
  font-weight: 700;
  color: #1d1e1f;
}

.page-copy {
  margin-top: 8px;
  color: #6b7280;
  line-height: 1.6;
}

.header-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.stats-row,
.filter-card {
  margin-bottom: 20px;
}

.stat-card {
  min-height: 110px;
}

.stat-label {
  color: #6b7280;
  font-size: 13px;
}

.stat-value {
  margin-top: 8px;
  color: #111827;
  font-size: 30px;
  font-weight: 700;
}

.stat-danger {
  color: #c85c55;
}

.filter-button {
  width: 100%;
}

.table-scroll {
  width: 100%;
  overflow-x: auto;
}

.table-scroll :deep(.el-table) {
  min-width: 1260px;
}

.issue-cell,
.stack-cell {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.issue-title {
  font-weight: 700;
  color: #111827;
}

.issue-content,
.stack-cell small {
  color: #6b7280;
  line-height: 1.5;
}

.issue-content {
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.empty-tip {
  padding: 40px 0;
}

.dialog-summary {
  margin-bottom: 18px;
  padding: 16px;
  border-radius: 12px;
  background: #f6f8fb;
  border: 1px solid #dbe3ef;
}

.dialog-title {
  font-weight: 700;
  color: #111827;
}

.dialog-copy {
  margin-top: 8px;
  color: #4b5563;
  line-height: 1.6;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
  }

  .header-actions,
  .header-actions .el-button {
    width: 100%;
  }
}
</style>
