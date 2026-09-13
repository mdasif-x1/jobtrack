/**
 * JobTrack Frontend Application Logic
 * Pure Vanilla JavaScript using same-origin relative `/api` endpoints.
 */

// Application State
const state = {
  activeView: 'dashboard',
  theme: localStorage.getItem('jobtrack_theme') || 'light',
  applications: {
    page: 0, // Zero-based API page index
    size: 10,
    sortField: 'applicationDate',
    sortOrder: 'desc',
    keyword: '',
    status: '',
    jobType: '',
    startDate: '',
    endDate: '',
    totalPages: 0,
    totalElements: 0
  },
  companies: [],
  deleteTarget: null // { type: 'app'|'company', id: number, label: string }
};

// API Helper Function
async function apiCall(endpoint, options = {}) {
  const config = {
    headers: {
      'Content-Type': 'application/json',
      ...options.headers
    },
    ...options
  };

  const response = await fetch(endpoint, config);

  // Handle 204 No Content
  if (response.status === 204) {
    return null;
  }

  const data = await response.json().catch(() => null);

  if (!response.ok) {
    const errorMsg = (data && data.message) ? data.message : `Server error (${response.status})`;
    const fieldErrors = (data && data.fieldErrors) ? data.fieldErrors : null;
    const error = new Error(errorMsg);
    error.status = response.status;
    error.fieldErrors = fieldErrors;
    throw error;
  }

  return data;
}

// Inline Section Loading Indicator Helper
function setSectionLoading(sectionId, isLoading) {
  const loaderEl = document.getElementById(`${sectionId}-loading`);
  if (loaderEl) {
    loaderEl.style.display = isLoading ? 'inline-flex' : 'none';
  }
}

function showToast(message, type = 'info') {
  const container = document.getElementById('toast-container');
  if (!container) return;

  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  toast.innerHTML = `
    <div class="toast-message">${escapeHtml(message)}</div>
    <button class="toast-close">&times;</button>
  `;

  toast.querySelector('.toast-close').addEventListener('click', () => toast.remove());

  container.appendChild(toast);
  setTimeout(() => {
    if (toast.parentNode) toast.remove();
  }, 4000);
}

function escapeHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

function formatDate(dateStr) {
  if (!dateStr) return '-';
  const date = new Date(dateStr);
  if (isNaN(date.getTime())) return dateStr;
  return date.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
}

function formatEnumLabel(enumVal) {
  if (!enumVal) return '';
  return enumVal.split('_').map(w => w.charAt(0) + w.slice(1).toLowerCase()).join(' ');
}

// Theme Management
function initTheme() {
  document.documentElement.setAttribute('data-theme', state.theme);
  updateThemeButton();
}

function toggleTheme() {
  state.theme = state.theme === 'light' ? 'dark' : 'light';
  localStorage.setItem('jobtrack_theme', state.theme);
  document.documentElement.setAttribute('data-theme', state.theme);
  updateThemeButton();
}

function updateThemeButton() {
  const icon = document.getElementById('theme-icon');
  const text = document.getElementById('theme-text');
  if (state.theme === 'dark') {
    if (icon) icon.textContent = '☀️';
    if (text) text.textContent = 'Light Mode';
  } else {
    if (icon) icon.textContent = '🌙';
    if (text) text.textContent = 'Dark Mode';
  }
}

// Navigation View Switcher
function switchView(viewName) {
  state.activeView = viewName;

  // Update Nav Active state
  document.querySelectorAll('.nav-item').forEach(btn => {
    if (btn.getAttribute('data-view') === viewName) {
      btn.classList.add('active');
    } else {
      btn.classList.remove('active');
    }
  });

  // Update View visibility
  document.querySelectorAll('.view-section').forEach(sec => {
    if (sec.id === `view-${viewName}`) {
      sec.classList.add('active');
    } else {
      sec.classList.remove('active');
    }
  });

  // Update Header Title
  const headerTitle = document.getElementById('header-title-text');
  if (headerTitle) {
    headerTitle.textContent = viewName.charAt(0).toUpperCase() + viewName.slice(1);
  }

  // Close mobile menu if open
  document.getElementById('sidebar').classList.remove('open');

  // Load view data
  if (viewName === 'dashboard') {
    loadDashboardData();
  } else if (viewName === 'applications') {
    loadCompaniesCache().then(() => loadApplicationsData());
  } else if (viewName === 'companies') {
    loadCompaniesData();
  }
}

// Company Helper & Loader
async function loadCompaniesCache() {
  try {
    const data = await apiCall('/api/companies');
    state.companies = data || [];
    populateCompanySelectDropdowns();
  } catch (err) {
    showToast(err.message || 'Failed to load companies list', 'error');
  }
}

function populateCompanySelectDropdowns() {
  const appCompanySelect = document.getElementById('app-companyId');
  if (!appCompanySelect) return;

  const currentVal = appCompanySelect.value;
  appCompanySelect.innerHTML = '<option value="">Select Company</option>';

  state.companies.forEach(comp => {
    const option = document.createElement('option');
    option.value = comp.id;
    option.textContent = comp.name + (comp.location ? ` (${comp.location})` : '');
    appCompanySelect.appendChild(option);
  });

  if (currentVal) {
    appCompanySelect.value = currentVal;
  }
}

// --- 1. DASHBOARD CONTROLLER ---
async function loadDashboardData() {
  setSectionLoading('dashboard', true);
  try {
    // Fetch stats and upcoming in parallel
    const [stats, upcoming] = await Promise.all([
      apiCall('/api/applications/statistics/status'),
      apiCall('/api/applications/upcoming')
    ]);

    renderDashboardStats(stats || {});
    renderDashboardUpcoming(upcoming || []);
  } catch (err) {
    showToast(err.message || 'Failed to load dashboard data', 'error');
  } finally {
    setSectionLoading('dashboard', false);
  }
}

function renderDashboardStats(stats) {
  const statuses = ['SAVED', 'APPLIED', 'ONLINE_ASSESSMENT', 'INTERVIEW', 'OFFERED', 'REJECTED', 'WITHDRAWN'];
  let totalApplications = 0;
  statuses.forEach(status => {
    const val = stats && stats[status] !== undefined ? Number(stats[status]) : 0;
    totalApplications += val;
    const el = document.getElementById(`stat-${status}`);
    if (el) {
      el.textContent = val;
    }
  });

  const totalEl = document.getElementById('stat-TOTAL');
  if (totalEl) {
    totalEl.textContent = totalApplications;
  }
}

function renderDashboardUpcoming(items) {
  const container = document.getElementById('upcoming-list-container');
  if (!container) return;

  if (items.length === 0) {
    container.innerHTML = `
      <div class="empty-state" style="padding: 24px;">
        <p>No upcoming action dates found.</p>
      </div>
    `;
    return;
  }

  container.innerHTML = items.map(item => `
    <div class="upcoming-item">
      <div class="upcoming-info">
        <span class="upcoming-role">${escapeHtml(item.jobRole)}</span>
        <span class="upcoming-company">${escapeHtml(item.company ? item.company.name : 'Unknown')}</span>
      </div>
      <div style="display: flex; align-items: center; gap: 12px;">
        <span class="badge badge-${escapeHtml(item.status)}">${escapeHtml(formatEnumLabel(item.status))}</span>
        <div class="upcoming-date">
          <span>📅</span> ${escapeHtml(formatDate(item.nextActionDate))}
        </div>
      </div>
    </div>
  `).join('');
}

// --- 2. APPLICATIONS CONTROLLER ---
async function loadApplicationsData() {
  setSectionLoading('applications', true);
  const { page, size, sortField, sortOrder, keyword, status, jobType, startDate, endDate } = state.applications;
  const sortParam = `${sortField},${sortOrder}`;

  let url = '';

  if (keyword && keyword.trim() !== '') {
    url = `/api/applications/search?keyword=${encodeURIComponent(keyword.trim())}&page=${page}&size=${size}&sort=${sortParam}`;
  } else if (status && status.trim() !== '') {
    url = `/api/applications/filter/status?status=${encodeURIComponent(status)}&page=${page}&size=${size}&sort=${sortParam}`;
  } else if (jobType && jobType.trim() !== '') {
    url = `/api/applications/filter/job-type?jobType=${encodeURIComponent(jobType)}&page=${page}&size=${size}&sort=${sortParam}`;
  } else if (startDate && endDate) {
    url = `/api/applications/filter/date?startDate=${encodeURIComponent(startDate)}&endDate=${encodeURIComponent(endDate)}&page=${page}&size=${size}&sort=${sortParam}`;
  } else {
    url = `/api/applications?page=${page}&size=${size}&sort=${sortParam}`;
  }

  try {
    const response = await apiCall(url);
    if (!response) return;

    state.applications.totalPages = response.totalPages;
    state.applications.totalElements = response.totalElements;

    renderApplicationsTable(response.content || []);
    renderApplicationsPagination(response);
  } catch (err) {
    showToast(err.message || 'Failed to load job applications', 'error');
  } finally {
    setSectionLoading('applications', false);
  }
}

function renderApplicationsTable(items) {
  const tbody = document.getElementById('applications-table-body');
  const emptyState = document.getElementById('applications-empty-state');
  if (!tbody || !emptyState) return;

  if (items.length === 0) {
    tbody.innerHTML = '';
    emptyState.style.display = 'flex';
    return;
  }

  emptyState.style.display = 'none';

  const statuses = ['SAVED', 'APPLIED', 'ONLINE_ASSESSMENT', 'INTERVIEW', 'OFFERED', 'REJECTED', 'WITHDRAWN'];

  tbody.innerHTML = items.map(app => {
    const statusOptions = statuses.map(s => 
      `<option value="${s}" ${app.status === s ? 'selected' : ''}>${formatEnumLabel(s)}</option>`
    ).join('');

    return `
      <tr>
        <td>
          <div style="font-weight: 600;">${escapeHtml(app.jobRole)}</div>
          ${app.jobLink ? `<a href="${escapeHtml(app.jobLink)}" target="_blank" rel="noopener" style="font-size: 12px; color: var(--primary);">View Posting ↗</a>` : ''}
        </td>
        <td>
          <div style="font-weight: 500;">${escapeHtml(app.company ? app.company.name : '-')}</div>
          <div style="font-size: 12px; color: var(--text-muted);">${escapeHtml(app.location || '')}</div>
        </td>
        <td>
          <select class="status-select-inline" onchange="updateApplicationStatusInline(${app.id}, this.value)">
            ${statusOptions}
          </select>
        </td>
        <td>
          <span class="badge badge-${escapeHtml(app.jobType)}">${escapeHtml(formatEnumLabel(app.jobType))}</span>
        </td>
        <td>${escapeHtml(formatDate(app.applicationDate))}</td>
        <td>${app.nextActionDate ? `<span style="font-size: 13px; font-weight: 500; color: var(--warning);">${escapeHtml(formatDate(app.nextActionDate))}</span>` : '-'}</td>
        <td>
          <div style="display: flex; gap: 6px;">
            <button class="btn btn-secondary btn-sm" onclick="openEditApplicationModal(${app.id})">Edit</button>
            <button class="btn btn-danger btn-sm" onclick="promptDeleteApplication(${app.id}, '${escapeHtml(app.jobRole)}')">Delete</button>
          </div>
        </td>
      </tr>
    `;
  }).join('');
}

function renderApplicationsPagination(pagedResponse) {
  const infoText = document.getElementById('pagination-info-text');
  const pageDisplay = document.getElementById('page-number-display');
  const prevBtn = document.getElementById('prev-page-btn');
  const nextBtn = document.getElementById('next-page-btn');

  const { page, size, totalElements, totalPages, first, last } = pagedResponse;

  const humanPage = page + 1;
  const maxPages = totalPages || 1;

  if (infoText) {
    const startItem = totalElements === 0 ? 0 : page * size + 1;
    const endItem = Math.min((page + 1) * size, totalElements);
    infoText.textContent = `Showing ${startItem}-${endItem} of ${totalElements} applications`;
  }

  if (pageDisplay) {
    pageDisplay.textContent = `Page ${humanPage} of ${maxPages}`;
  }

  if (prevBtn) prevBtn.disabled = first || humanPage <= 1;
  if (nextBtn) nextBtn.disabled = last || humanPage >= maxPages;
}

// Inline Status Update (PATCH)
async function updateApplicationStatusInline(id, newStatus) {
  try {
    await apiCall(`/api/applications/${id}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ status: newStatus })
    });
    showToast('Application status updated successfully', 'success');
  } catch (err) {
    showToast(err.message || 'Failed to update status', 'error');
    // Refresh to restore valid state
    loadApplicationsData();
  }
}

// --- 3. COMPANIES CONTROLLER ---
async function loadCompaniesData() {
  setSectionLoading('companies', true);
  try {
    const data = await apiCall('/api/companies');
    state.companies = data || [];
    renderCompaniesGrid(state.companies);
  } catch (err) {
    showToast(err.message || 'Failed to load companies', 'error');
  } finally {
    setSectionLoading('companies', false);
  }
}

function renderCompaniesGrid(companies) {
  const container = document.getElementById('companies-grid');
  const emptyState = document.getElementById('companies-empty-state');
  if (!container || !emptyState) return;

  if (companies.length === 0) {
    container.innerHTML = '';
    emptyState.style.display = 'flex';
    return;
  }

  emptyState.style.display = 'none';

  container.innerHTML = companies.map(comp => `
    <div class="company-card">
      <div>
        <h3 class="company-name">${escapeHtml(comp.name)}</h3>
        ${comp.location ? `<div class="company-detail">📍 ${escapeHtml(comp.location)}</div>` : ''}
        ${comp.website ? `<div class="company-detail" style="margin-top: 6px;"><a href="${escapeHtml(comp.website)}" target="_blank" rel="noopener" style="color: var(--primary);">🌐 ${escapeHtml(comp.website)}</a></div>` : ''}
      </div>
      <div class="company-actions">
        <button class="btn btn-secondary btn-sm" onclick="openEditCompanyModal(${comp.id})">Edit</button>
        <button class="btn btn-danger btn-sm" onclick="promptDeleteCompany(${comp.id}, '${escapeHtml(comp.name)}')">Delete</button>
      </div>
    </div>
  `).join('');
}

// --- FORM HANDLING & MODALS ---

// Clear errors helper
function clearFormErrors(formId) {
  const form = document.getElementById(formId);
  if (!form) return;

  form.querySelectorAll('.field-error-msg').forEach(el => el.textContent = '');
  const alert = form.querySelector('.alert-error');
  if (alert) {
    alert.style.display = 'none';
    alert.textContent = '';
  }
}

function displayFormErrors(formId, error) {
  const form = document.getElementById(formId);
  if (!form) return;

  const alert = form.querySelector('.alert-error');
  if (alert && error.message) {
    alert.textContent = error.message;
    alert.style.display = 'block';
  }

  if (error.fieldErrors) {
    Object.keys(error.fieldErrors).forEach(field => {
      const errEl = form.querySelector(`#err-${field}`) || form.querySelector(`#err-comp-${field}`);
      if (errEl) {
        errEl.textContent = error.fieldErrors[field];
      }
    });
  }
}

// Application Modal Handlers
function openCreateApplicationModal() {
  clearFormErrors('application-form');
  document.getElementById('application-form').reset();
  document.getElementById('app-id').value = '';
  document.getElementById('app-modal-title').textContent = 'New Application';

  // Default date to today
  document.getElementById('app-applicationDate').value = new Date().toISOString().split('T')[0];

  populateCompanySelectDropdowns();
  document.getElementById('app-modal').classList.add('active');
}

async function openEditApplicationModal(id) {
  clearFormErrors('application-form');
  document.getElementById('app-modal-title').textContent = 'Edit Application';

  try {
    await loadCompaniesCache();
    const app = await apiCall(`/api/applications?page=0&size=100`); // Fetch application list to find item or add get if available
    const item = app.content.find(a => a.id === id);

    if (!item) {
      showToast('Application not found', 'error');
      return;
    }

    document.getElementById('app-id').value = item.id;
    document.getElementById('app-jobRole').value = item.jobRole || '';
    document.getElementById('app-companyId').value = item.company ? item.company.id : '';
    document.getElementById('app-status').value = item.status || 'APPLIED';
    document.getElementById('app-jobType').value = item.jobType || 'FULL_TIME';
    document.getElementById('app-applicationDate').value = item.applicationDate || '';
    document.getElementById('app-nextActionDate').value = item.nextActionDate || '';
    document.getElementById('app-jobLink').value = item.jobLink || '';
    document.getElementById('app-location').value = item.location || '';
    document.getElementById('app-notes').value = item.notes || '';

    document.getElementById('app-modal').classList.add('active');
  } catch (err) {
    showToast(err.message || 'Failed to load application details', 'error');
  }
}

function closeApplicationModal() {
  document.getElementById('app-modal').classList.remove('active');
}

// Save Application Form Submit
async function handleApplicationFormSubmit(e) {
  e.preventDefault();
  clearFormErrors('application-form');

  const appId = document.getElementById('app-id').value;
  const isEdit = !!appId;

  const payload = {
    jobRole: document.getElementById('app-jobRole').value.trim(),
    companyId: parseInt(document.getElementById('app-companyId').value, 10),
    status: document.getElementById('app-status').value,
    jobType: document.getElementById('app-jobType').value,
    applicationDate: document.getElementById('app-applicationDate').value,
    jobLink: document.getElementById('app-jobLink').value.trim(),
    location: document.getElementById('app-location').value.trim() || null,
    nextActionDate: document.getElementById('app-nextActionDate').value || null,
    notes: document.getElementById('app-notes').value.trim() || null
  };

  try {
    const endpoint = isEdit ? `/api/applications/${appId}` : '/api/applications';
    const method = isEdit ? 'PUT' : 'POST';

    await apiCall(endpoint, {
      method: method,
      body: JSON.stringify(payload)
    });

    showToast(`Application ${isEdit ? 'updated' : 'created'} successfully`, 'success');
    closeApplicationModal();
    loadApplicationsData();
  } catch (err) {
    displayFormErrors('application-form', err);
  }
}

// Company Modal Handlers
function openCreateCompanyModal() {
  clearFormErrors('company-form');
  document.getElementById('company-form').reset();
  document.getElementById('company-id').value = '';
  document.getElementById('company-modal-title').textContent = 'Add Company';
  document.getElementById('company-modal').classList.add('active');
}

function openEditCompanyModal(id) {
  clearFormErrors('company-form');
  const comp = state.companies.find(c => c.id === id);
  if (!comp) return;

  document.getElementById('company-id').value = comp.id;
  document.getElementById('company-name').value = comp.name || '';
  document.getElementById('company-website').value = comp.website || '';
  document.getElementById('company-location').value = comp.location || '';
  document.getElementById('company-modal-title').textContent = 'Edit Company';

  document.getElementById('company-modal').classList.add('active');
}

function closeCompanyModal() {
  document.getElementById('company-modal').classList.remove('active');
}

async function handleCompanyFormSubmit(e) {
  e.preventDefault();
  clearFormErrors('company-form');

  const compId = document.getElementById('company-id').value;
  const isEdit = !!compId;

  const payload = {
    name: document.getElementById('company-name').value.trim(),
    website: document.getElementById('company-website').value.trim(),
    location: document.getElementById('company-location').value.trim() || null
  };

  try {
    const endpoint = isEdit ? `/api/companies/${compId}` : '/api/companies';
    const method = isEdit ? 'PUT' : 'POST';

    await apiCall(endpoint, {
      method: method,
      body: JSON.stringify(payload)
    });

    showToast(`Company ${isEdit ? 'updated' : 'created'} successfully`, 'success');
    closeCompanyModal();
    loadCompaniesData();
    loadCompaniesCache();
  } catch (err) {
    displayFormErrors('company-form', err);
  }
}

// Delete Confirmation Modal Handlers
function promptDeleteApplication(id, label) {
  state.deleteTarget = { type: 'app', id, label };
  document.getElementById('confirm-modal-message').textContent = `Are you sure you want to delete application "${label}"?`;
  document.getElementById('confirm-modal').classList.add('active');
}

function promptDeleteCompany(id, label) {
  state.deleteTarget = { type: 'company', id, label };
  document.getElementById('confirm-modal-message').textContent = `Are you sure you want to delete company "${label}"?`;
  document.getElementById('confirm-modal').classList.add('active');
}

function closeConfirmModal() {
  state.deleteTarget = null;
  document.getElementById('confirm-modal').classList.remove('active');
}

async function handleConfirmDelete() {
  if (!state.deleteTarget) return;

  const { type, id, label } = state.deleteTarget;
  closeConfirmModal();

  try {
    if (type === 'app') {
      await apiCall(`/api/applications/${id}`, { method: 'DELETE' });
      showToast(`Application "${label}" deleted`, 'success');
      loadApplicationsData();
    } else if (type === 'company') {
      await apiCall(`/api/companies/${id}`, { method: 'DELETE' });
      showToast(`Company "${label}" deleted`, 'success');
      loadCompaniesData();
      loadCompaniesCache();
    }
  } catch (err) {
    // Displays server error message (e.g. 409 Conflict if company in use)
    showToast(err.message || 'Deletion failed', 'error');
  }
}

// Event Listeners Setup
function setupEventListeners() {
  // Theme toggle
  document.getElementById('theme-toggle-btn').addEventListener('click', toggleTheme);

  // Mobile menu toggle
  document.getElementById('mobile-toggle-btn').addEventListener('click', () => {
    document.getElementById('sidebar').classList.toggle('open');
  });

  // Navigation tabs
  document.querySelectorAll('.nav-item').forEach(btn => {
    btn.addEventListener('click', (e) => {
      const view = e.currentTarget.getAttribute('data-view');
      switchView(view);
    });
  });

  // Application Modal open/close
  document.getElementById('open-create-app-btn').addEventListener('click', openCreateApplicationModal);
  document.querySelectorAll('.close-app-modal-btn').forEach(btn => {
    btn.addEventListener('click', closeApplicationModal);
  });
  document.getElementById('application-form').addEventListener('submit', handleApplicationFormSubmit);

  // Company Modal open/close
  document.getElementById('open-create-company-btn').addEventListener('click', openCreateCompanyModal);
  document.querySelectorAll('.close-company-modal-btn').forEach(btn => {
    btn.addEventListener('click', closeCompanyModal);
  });
  document.getElementById('company-form').addEventListener('submit', handleCompanyFormSubmit);

  // Confirm Delete Modal actions
  document.getElementById('close-confirm-modal-btn').addEventListener('click', closeConfirmModal);
  document.getElementById('cancel-confirm-btn').addEventListener('click', closeConfirmModal);
  document.getElementById('action-confirm-btn').addEventListener('click', handleConfirmDelete);

  // Filters & Controls Listeners
  let searchTimeout = null;
  document.getElementById('filter-search').addEventListener('input', (e) => {
    clearTimeout(searchTimeout);
    searchTimeout = setTimeout(() => {
      state.applications.keyword = e.target.value;
      state.applications.page = 0;
      loadApplicationsData();
    }, 400);
  });

  document.getElementById('filter-status').addEventListener('change', (e) => {
    state.applications.status = e.target.value;
    state.applications.page = 0;
    loadApplicationsData();
  });

  document.getElementById('filter-jobtype').addEventListener('change', (e) => {
    state.applications.jobType = e.target.value;
    state.applications.page = 0;
    loadApplicationsData();
  });

  const handleDateChange = () => {
    const start = document.getElementById('filter-start-date').value;
    const end = document.getElementById('filter-end-date').value;
    state.applications.startDate = start;
    state.applications.endDate = end;
    if ((start && end) || (!start && !end)) {
      state.applications.page = 0;
      loadApplicationsData();
    }
  };

  document.getElementById('filter-start-date').addEventListener('change', handleDateChange);
  document.getElementById('filter-end-date').addEventListener('change', handleDateChange);

  document.getElementById('reset-filters-btn').addEventListener('click', () => {
    document.getElementById('filter-search').value = '';
    document.getElementById('filter-status').value = '';
    document.getElementById('filter-jobtype').value = '';
    document.getElementById('filter-start-date').value = '';
    document.getElementById('filter-end-date').value = '';

    state.applications.keyword = '';
    state.applications.status = '';
    state.applications.jobType = '';
    state.applications.startDate = '';
    state.applications.endDate = '';
    state.applications.page = 0;

    loadApplicationsData();
  });

  // Sort & Page Size Controls
  document.getElementById('sort-field').addEventListener('change', (e) => {
    state.applications.sortField = e.target.value;
    state.applications.page = 0;
    loadApplicationsData();
  });

  document.getElementById('sort-order').addEventListener('change', (e) => {
    state.applications.sortOrder = e.target.value;
    state.applications.page = 0;
    loadApplicationsData();
  });

  document.getElementById('page-size-select').addEventListener('change', (e) => {
    state.applications.size = parseInt(e.target.value, 10);
    state.applications.page = 0;
    loadApplicationsData();
  });

  // Pagination buttons
  document.getElementById('prev-page-btn').addEventListener('click', () => {
    if (state.applications.page > 0) {
      state.applications.page--;
      loadApplicationsData();
    }
  });

  document.getElementById('next-page-btn').addEventListener('click', () => {
    if (state.applications.page < state.applications.totalPages - 1) {
      state.applications.page++;
      loadApplicationsData();
    }
  });
}

// Initial Bootstrapping
document.addEventListener('DOMContentLoaded', () => {
  initTheme();
  setupEventListeners();
  switchView('dashboard');
});
