/**
 * Windows 11 Debloater — Frontend Application
 *
 * All action data comes from Java (ActionRegistry) via window.bridge.getActionsJson().
 * No action data is hardcoded here.
 * JavaScript only sends action IDs to Java, never raw PowerShell.
 * Java validates all IDs and generates PowerShell.
 */

// ============================================================
//  State
// ============================================================

/** @type {Array<Object>} All actions loaded from Java */
let allActions = [];

/** @type {Map<string, boolean>} Toggle state for each action ID */
let selectionState = new Map();

/** @type {string|null} Currently active category filter (null = show all) */
let activeCategory = null;

/** @type {string} Current search query */
let searchQuery = '';

/** @type {boolean} Whether the app is running as admin */
let isAdmin = false;

/** @type {boolean} Whether an execution is in progress */
let isExecuting = false;

/** @type {Map<string, Object>} Action results by ID, populated during execution */
let actionResults = new Map();

// ============================================================
//  Bridge Ready Callback
//  Called by Java after the bridge is injected into window.bridge.
// ============================================================

function onBridgeReady() {
    // Load admin status
    try {
        var adminStatus = JSON.parse(window.bridge.getAdminStatus());
        isAdmin = adminStatus.isAdmin;
        updateAdminUI();
    } catch (e) {
        console.error('Failed to get admin status:', e);
    }

    // Load all actions from Java
    try {
        var actionsJson = window.bridge.getActionsJson();
        allActions = JSON.parse(actionsJson);

        // Initialize selection state from defaults
        allActions.forEach(function(action) {
            selectionState.set(action.id, action.selectedByDefault);
        });

        // Build the UI
        buildSidebar();
        renderActions();
        updateSelectionCount();
    } catch (e) {
        console.error('Failed to load actions:', e);
        document.getElementById('contentArea').innerHTML =
            '<div class="no-results">Failed to load actions from backend. ' + e.message + '</div>';
    }
}

// ============================================================
//  Admin UI
// ============================================================

function updateAdminUI() {
    var badge = document.getElementById('adminBadge');
    var warning = document.getElementById('adminWarning');
    var runBtn = document.getElementById('btnRun');

    if (isAdmin) {
        badge.className = 'admin-badge admin-yes';
        badge.textContent = 'Administrator';
        warning.classList.remove('active');
        runBtn.disabled = false;
        runBtn.className = 'btn btn-primary';
        runBtn.textContent = 'Run Selected';
    } else {
        badge.className = 'admin-badge admin-no';
        badge.textContent = 'Not Admin';
        badge.onclick = restartAsAdmin;
        warning.classList.add('active');
        // Button stays clickable but visually shows it needs admin
        runBtn.disabled = false;
        runBtn.className = 'btn btn-run-blocked';
        runBtn.textContent = 'Run Selected (Requires Admin)';
    }
}

function restartAsAdmin() {
    if (window.bridge) {
        window.bridge.restartAsAdmin();
    }
}

/**
 * Shows a prominent modal explaining that admin privileges are required.
 * Offers a "Restart as Admin" button right in the modal.
 */
function showAdminRequiredModal() {
    openModal('adminRequiredModal');
}

// ============================================================
//  Sidebar Navigation
// ============================================================

function buildSidebar() {
    var nav = document.getElementById('sidebarNav');
    nav.innerHTML = '';

    // "All" item
    var allItem = document.createElement('div');
    allItem.className = 'nav-item' + (activeCategory === null ? ' active' : '');
    allItem.innerHTML = '<span>All Actions</span><span class="count">' + allActions.length + '</span>';
    allItem.onclick = function() { setActiveCategory(null); };
    nav.appendChild(allItem);

    // Group by category (maintain order)
    var categories = [];
    var categoryCounts = {};
    allActions.forEach(function(a) {
        if (!categoryCounts[a.category]) {
            categoryCounts[a.category] = 0;
            categories.push({ label: a.category, order: a.categoryOrder });
        }
        categoryCounts[a.category]++;
    });
    categories.sort(function(a, b) { return a.order - b.order; });

    categories.forEach(function(cat) {
        var item = document.createElement('div');
        item.className = 'nav-item' + (activeCategory === cat.label ? ' active' : '');
        item.innerHTML = '<span>' + escapeHtml(cat.label) + '</span><span class="count">' + categoryCounts[cat.label] + '</span>';
        item.onclick = function() { setActiveCategory(cat.label); };
        nav.appendChild(item);
    });
}

function setActiveCategory(category) {
    activeCategory = category;
    buildSidebar();
    renderActions();
}

// ============================================================
//  Search / Filter
// ============================================================

function handleSearch(query) {
    searchQuery = query.toLowerCase().trim();
    renderActions();
}

function matchesSearch(action) {
    if (!searchQuery) return true;
    if (action.title.toLowerCase().indexOf(searchQuery) >= 0) return true;
    if (action.description.toLowerCase().indexOf(searchQuery) >= 0) return true;
    if (action.category.toLowerCase().indexOf(searchQuery) >= 0) return true;
    if (action.id.toLowerCase().indexOf(searchQuery) >= 0) return true;
    for (var i = 0; i < action.tags.length; i++) {
        if (action.tags[i].toLowerCase().indexOf(searchQuery) >= 0) return true;
    }
    return false;
}

// ============================================================
//  Render Actions
// ============================================================

function renderActions() {
    var container = document.getElementById('contentArea');
    container.innerHTML = '';

    // Filter actions
    var filtered = allActions.filter(function(a) {
        if (activeCategory && a.category !== activeCategory) return false;
        return matchesSearch(a);
    });

    if (filtered.length === 0) {
        container.innerHTML = '<div class="no-results">No actions match your search.</div>';
        return;
    }

    // Group by category
    var groups = {};
    var groupOrder = [];
    filtered.forEach(function(a) {
        if (!groups[a.category]) {
            groups[a.category] = [];
            groupOrder.push({ label: a.category, order: a.categoryOrder });
        }
        groups[a.category].push(a);
    });
    groupOrder.sort(function(a, b) { return a.order - b.order; });

    groupOrder.forEach(function(cat) {
        var actions = groups[cat.label];
        var group = document.createElement('div');
        group.className = 'category-group';

        // Category header with collapse toggle
        var header = document.createElement('div');
        header.className = 'category-header';
        header.innerHTML =
            '<h3><span class="collapse-icon" id="collapseIcon_' + cssId(cat.label) + '">&#9660;</span> ' +
            escapeHtml(cat.label) + ' <span style="color:var(--text-muted);font-size:12px;font-weight:400;">(' + actions.length + ')</span></h3>' +
            '<span class="category-toggle-all" onclick="event.stopPropagation(); toggleCategory(\'' + escapeAttr(cat.label) + '\')">Toggle All</span>';
        header.onclick = function() { collapseCategory(cat.label); };
        group.appendChild(header);

        // Actions grid
        var grid = document.createElement('div');
        grid.className = 'category-actions';
        grid.id = 'grid_' + cssId(cat.label);

        actions.forEach(function(action) {
            grid.appendChild(createActionCard(action));
        });

        group.appendChild(grid);
        container.appendChild(group);
    });
}

function createActionCard(action) {
    var card = document.createElement('div');
    var riskClass = action.riskLevel.toLowerCase();
    var isSelected = selectionState.get(action.id) || false;
    var result = actionResults.get(action.id);
    var hasResult = !!result;

    // Base card class includes risk level stripe
    var cardClass = 'action-card risk-' + riskClass;

    // After execution: add result state class for full-card coloring
    if (hasResult) {
        cardClass += ' result-' + statusToCssClass(result.status);
    } else if (isExecuting && isSelected) {
        cardClass += ' result-pending';
    }

    card.className = cardClass;
    card.id = 'card_' + action.id;

    // Left side: toggle switch (before execution) or large result icon (after execution)
    var leftHtml;
    if (hasResult) {
        // Show a large green check / red X / etc. instead of the toggle
        leftHtml = renderResultIcon(result.status);
    } else if (isExecuting && isSelected) {
        // Show pending/spinner icon during execution
        leftHtml = '<div class="result-icon icon-pending"><div class="spinner"></div></div>';
    } else {
        // Normal toggle switch for selection
        leftHtml =
            '<label class="toggle-switch' + (riskClass === 'aggressive' ? ' aggressive' : '') + '">' +
            '<input type="checkbox" id="toggle_' + action.id + '" ' + (isSelected ? 'checked' : '') +
            ' onchange="toggleAction(\'' + action.id + '\', this.checked)">' +
            '<span class="toggle-slider"></span></label>';
    }

    // Right side: card content
    var contentHtml =
        '<div class="action-card-content">' +
        '<div class="action-card-header">' +
        '<span class="action-card-title">' + escapeHtml(action.title) + '</span>' +
        '<span class="risk-badge ' + riskClass + '">' + escapeHtml(action.riskLevel) + '</span>' +
        '</div>' +
        '<div class="action-card-desc">' + escapeHtml(action.description) + '</div>';

    // Status detail text below description (after execution)
    if (hasResult) {
        contentHtml += renderStatusDetail(result);
    } else if (isExecuting && isSelected) {
        contentHtml += '<div class="action-status-detail status-pending">Waiting to run...</div>';
    }

    contentHtml += '</div>';

    card.innerHTML = leftHtml + contentHtml;
    return card;
}

/**
 * Renders a large circular icon reflecting the action's result status.
 * Green checkmark for success, red X for failed, etc.
 */
function renderResultIcon(status) {
    var iconClass = 'icon-pending';
    var symbol = '?';
    switch (status) {
        case 'Success':
            iconClass = 'icon-success';
            symbol = '\u2713';  // checkmark
            break;
        case 'Failed':
            iconClass = 'icon-failed';
            symbol = '\u2717';  // X mark
            break;
        case 'Skipped':
            iconClass = 'icon-skipped';
            symbol = '\u2014';  // em dash
            break;
        case 'Partially completed':
            iconClass = 'icon-partial';
            symbol = '!';
            break;
        case 'Already applied':
            iconClass = 'icon-already-applied';
            symbol = '\u2713';  // checkmark
            break;
    }
    return '<div class="result-icon ' + iconClass + '">' + symbol + '</div>';
}

/**
 * Renders the status detail text below the action description.
 * Shows status label, message, and error if any.
 */
function renderStatusDetail(result) {
    var cls = 'status-' + statusToCssClass(result.status);
    var html = '<div class="action-status-detail ' + cls + '">';
    html += '<strong>' + escapeHtml(result.status) + '</strong>';
    if (result.message) {
        html += ' — ' + escapeHtml(result.message);
    }
    if (result.error) {
        html += '<div class="status-error">Error: ' + escapeHtml(result.error) + '</div>';
    }
    html += '</div>';
    return html;
}

/**
 * Maps a result status string to a CSS class name.
 */
function statusToCssClass(status) {
    switch (status) {
        case 'Success': return 'success';
        case 'Failed': return 'failed';
        case 'Skipped': return 'skipped';
        case 'Partially completed': return 'partial';
        case 'Already applied': return 'already-applied';
        default: return 'pending';
    }
}

// ============================================================
//  Toggle Handling
// ============================================================

function toggleAction(id, checked) {
    selectionState.set(id, checked);
    updateSelectionCount();
}

function toggleCategory(categoryLabel) {
    var actions = allActions.filter(function(a) { return a.category === categoryLabel; });
    // If all are selected, deselect all. Otherwise, select all.
    var allSelected = actions.every(function(a) { return selectionState.get(a.id); });
    actions.forEach(function(a) {
        selectionState.set(a.id, !allSelected);
    });
    renderActions();
    updateSelectionCount();
}

function collapseCategory(categoryLabel) {
    var gridId = 'grid_' + cssId(categoryLabel);
    var iconId = 'collapseIcon_' + cssId(categoryLabel);
    var grid = document.getElementById(gridId);
    var icon = document.getElementById(iconId);
    if (grid) {
        grid.classList.toggle('collapsed');
    }
    if (icon) {
        icon.classList.toggle('collapsed');
    }
}

function selectRecommended() {
    allActions.forEach(function(a) {
        selectionState.set(a.id, a.selectedByDefault);
    });
    renderActions();
    updateSelectionCount();
}

function selectAll() {
    allActions.forEach(function(a) {
        selectionState.set(a.id, true);
    });
    renderActions();
    updateSelectionCount();
}

function selectNone() {
    allActions.forEach(function(a) {
        selectionState.set(a.id, false);
    });
    renderActions();
    updateSelectionCount();
}

function getSelectedIds() {
    var ids = [];
    selectionState.forEach(function(selected, id) {
        if (selected) ids.push(id);
    });
    return ids;
}

function updateSelectionCount() {
    var count = getSelectedIds().length;
    document.getElementById('selectionCount').textContent = count + ' selected';
}

// ============================================================
//  Profile Import/Export
// ============================================================

function exportProfile() {
    if (!window.bridge) return;
    var ids = getSelectedIds();
    var result = JSON.parse(window.bridge.exportProfile(JSON.stringify(ids)));
    if (result.success) {
        showToast('Profile exported to ' + result.path);
    } else if (result.error !== 'Export cancelled.') {
        showToast('Export failed: ' + result.error);
    }
}

function importProfile() {
    if (!window.bridge) return;
    var result = JSON.parse(window.bridge.importProfile());
    if (result.success) {
        // Apply imported selection
        selectNone(); // Clear all first
        result.validIds.forEach(function(id) {
            selectionState.set(id, true);
        });
        renderActions();
        updateSelectionCount();

        var msg = 'Imported ' + result.validIds.length + ' actions from ' + result.path;
        if (result.unknownIds.length > 0) {
            msg += '. Ignored ' + result.unknownIds.length + ' unknown action ID(s).';
        }
        showToast(msg);
    } else if (result.error !== 'Import cancelled.') {
        showToast('Import failed: ' + result.error);
    }
}

// ============================================================
//  Preview
// ============================================================

function previewScript() {
    if (!window.bridge) return;
    var ids = getSelectedIds();
    if (ids.length === 0) {
        showToast('No actions selected to preview.');
        return;
    }
    var script = window.bridge.previewScript(JSON.stringify(ids));
    document.getElementById('previewContent').textContent = script;
    openModal('previewModal');
}

function copyPreview() {
    var content = document.getElementById('previewContent').textContent;
    if (navigator.clipboard) {
        navigator.clipboard.writeText(content).then(function() {
            showToast('Copied to clipboard.');
        });
    } else {
        // Fallback: select and copy
        var range = document.createRange();
        range.selectNodeContents(document.getElementById('previewContent'));
        window.getSelection().removeAllRanges();
        window.getSelection().addRange(range);
        document.execCommand('copy');
        window.getSelection().removeAllRanges();
        showToast('Copied to clipboard.');
    }
}

// ============================================================
//  Execution
// ============================================================

function runSelected() {
    if (!isAdmin) {
        showAdminRequiredModal();
        return;
    }
    if (isExecuting) {
        showToast('Execution already in progress.');
        return;
    }
    var ids = getSelectedIds();
    if (ids.length === 0) {
        showToast('No actions selected.');
        return;
    }

    // Check for aggressive actions to show warning
    var aggressive = allActions.filter(function(a) {
        return selectionState.get(a.id) && a.riskLevel === 'Aggressive';
    });

    var msg = 'Run ' + ids.length + ' selected action(s)?';
    document.getElementById('confirmMessage').textContent = msg;

    if (aggressive.length > 0) {
        document.getElementById('aggressiveWarning').classList.remove('hidden');
        var listHtml = '';
        aggressive.forEach(function(a) {
            listHtml += '<div class="aggressive-item">' + escapeHtml(a.title) + '</div>';
        });
        document.getElementById('aggressiveList').innerHTML = listHtml;
    } else {
        document.getElementById('aggressiveWarning').classList.add('hidden');
    }

    openModal('confirmModal');
}

function confirmRun() {
    closeModal('confirmModal');

    var ids = getSelectedIds();
    isExecuting = true;
    actionResults.clear();

    // Enable log panel
    document.getElementById('logPanel').classList.add('active');
    document.getElementById('logContent').innerHTML = '';

    // Disable buttons during execution
    setExecutionButtons(true);

    // Update UI to show pending status
    renderActions();

    // Call Java bridge
    var result = JSON.parse(window.bridge.runSelected(JSON.stringify(ids)));
    if (!result.success) {
        showToast('Execution failed: ' + result.error);
        isExecuting = false;
        setExecutionButtons(false);
    }
}

function setExecutionButtons(executing) {
    document.getElementById('btnRun').disabled = executing || !isAdmin;
    document.getElementById('btnSaveLog').disabled = executing;
    document.getElementById('btnExportJson').disabled = executing;
    document.getElementById('btnSaveTxt').disabled = executing;
}

// ============================================================
//  Live Callbacks from Java (called during execution)
// ============================================================

/** Called by Java for each stdout/stderr line */
function onRawLogLine(line) {
    var logEl = document.getElementById('logContent');
    var lineEl = document.createElement('div');

    // Color-code log lines
    if (line.indexOf('[RESULT]') === 0) {
        lineEl.className = 'log-line-result';
    } else if (line.indexOf('[ERROR]') === 0 || line.indexOf('[STDERR]') === 0) {
        lineEl.className = 'log-line-error';
    } else if (line.indexOf('[INFO]') === 0) {
        lineEl.className = 'log-line-info';
    } else if (line.indexOf('[WARN]') === 0) {
        lineEl.className = 'log-line-warn';
    }

    lineEl.textContent = line;
    logEl.appendChild(lineEl);
    logEl.scrollTop = logEl.scrollHeight;
}

/** Called by Java for each parsed action result */
function onActionResult(resultJson) {
    try {
        var result = JSON.parse(resultJson);
        actionResults.set(result.id, result);

        // Update the specific card
        var card = document.getElementById('card_' + result.id);
        if (card) {
            var action = allActions.find(function(a) { return a.id === result.id; });
            if (action) {
                var newCard = createActionCard(action);
                card.parentNode.replaceChild(newCard, card);
            }
        }
    } catch (e) {
        console.error('Failed to parse action result:', e);
    }
}

/** Called by Java when all actions have finished */
function onExecutionComplete(summaryJson, resultsJson) {
    isExecuting = false;
    setExecutionButtons(false);

    try {
        var summary = JSON.parse(summaryJson);
        var results = JSON.parse(resultsJson);
        showReport(summary, results);
    } catch (e) {
        console.error('Failed to parse execution results:', e);
        showToast('Execution complete, but failed to parse results.');
    }
}

/** Called by Java on execution error */
function onExecutionError(errorMsg) {
    showToast('Execution error: ' + errorMsg);
}

// ============================================================
//  Report
// ============================================================

function showReport(summary, results) {
    var body = document.getElementById('reportBody');
    var html = '';

    // Summary stats
    html += '<div class="report-summary">';
    html += statBox('total', 'Total', summary.total);
    html += statBox('success', 'Success', summary.successCount);
    html += statBox('failed', 'Failed', summary.failedCount);
    html += statBox('skipped', 'Skipped', summary.skippedCount);
    html += statBox('partial', 'Partial', summary.partialCount);
    html += statBox('applied', 'Already Applied', summary.alreadyAppliedCount);
    html += '</div>';

    // Restart warning
    if (summary.restartRecommended) {
        html += '<div style="padding:10px;margin-bottom:16px;background:rgba(255,165,2,0.1);border:1px solid rgba(255,165,2,0.3);border-radius:4px;color:#ffa502;font-size:13px;">';
        html += '<strong>Restart recommended</strong> for all changes to fully apply.<br>';
        html += 'Major Windows feature updates may restore some of these settings. Re-run the selected profile after major Windows updates if needed.';
        html += '</div>';
    }

    // Group results by category
    var byCategory = {};
    var categoryOrder = [];
    results.forEach(function(r) {
        if (!byCategory[r.category]) {
            byCategory[r.category] = [];
            categoryOrder.push(r.category);
        }
        byCategory[r.category].push(r);
    });

    categoryOrder.forEach(function(cat) {
        html += '<div class="report-category-title">' + escapeHtml(cat) + '</div>';
        byCategory[cat].forEach(function(r) {
            html += renderReportAction(r);
        });
    });

    body.innerHTML = html;
    openModal('reportModal');
}

function statBox(cls, label, value) {
    return '<div class="report-stat ' + cls + '">' +
        '<div class="stat-value">' + value + '</div>' +
        '<div class="stat-label">' + escapeHtml(label) + '</div></div>';
}

function renderReportAction(r) {
    var icon = '?';
    switch (r.status) {
        case 'Success': icon = '\u2705'; break;
        case 'Failed': icon = '\u274c'; break;
        case 'Skipped': icon = '\u23ed'; break;
        case 'Partially completed': icon = '\u26a0\ufe0f'; break;
        case 'Already applied': icon = '\u2611'; break;
    }

    var html = '<div class="report-action">';
    html += '<div class="report-action-header">';
    html += '<span>' + icon + '</span>';
    html += '<span>' + escapeHtml(r.title) + '</span>';
    html += '<span class="risk-badge ' + r.riskLevel.toLowerCase() + '">' + escapeHtml(r.riskLevel) + '</span>';
    html += '</div>';
    html += '<div class="report-action-detail">Status: ' + escapeHtml(r.status) + '</div>';
    if (r.message) {
        html += '<div class="report-action-detail">' + escapeHtml(r.message) + '</div>';
    }
    if (r.details) {
        html += '<div class="report-action-detail" style="color:var(--text-muted);">' + escapeHtml(r.details) + '</div>';
    }
    if (r.error) {
        html += '<div class="report-action-error">Error: ' + escapeHtml(r.error) + '</div>';
    }
    html += '</div>';
    return html;
}

// ============================================================
//  Save / Export (bottom bar buttons)
// ============================================================

function saveLog() {
    if (!window.bridge) return;
    var result = JSON.parse(window.bridge.saveLog());
    if (result.success) showToast('Log saved to ' + result.path);
}

function exportReportJson() {
    if (!window.bridge) return;
    var result = JSON.parse(window.bridge.exportReportJson());
    if (result.success) showToast('JSON report exported to ' + result.path);
}

function saveReportTxt() {
    if (!window.bridge) return;
    var result = JSON.parse(window.bridge.saveReportTxt());
    if (result.success) showToast('Text report saved to ' + result.path);
}

// ============================================================
//  Log Panel
// ============================================================

function toggleLogPanel() {
    document.getElementById('logPanel').classList.toggle('active');
}

// ============================================================
//  Modal Helpers
// ============================================================

function openModal(id) {
    document.getElementById(id).classList.add('active');
}

function closeModal(id) {
    document.getElementById(id).classList.remove('active');
}

// Close modals on overlay click
document.addEventListener('click', function(e) {
    if (e.target.classList.contains('modal-overlay') && e.target.classList.contains('active')) {
        e.target.classList.remove('active');
    }
});

// Close modals on Escape key
document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape') {
        document.querySelectorAll('.modal-overlay.active').forEach(function(m) {
            m.classList.remove('active');
        });
    }
});

// ============================================================
//  Toast Notification (simple bottom-right message)
// ============================================================

function showToast(message) {
    // Remove existing toast if any
    var existing = document.getElementById('toastMsg');
    if (existing) existing.remove();

    var toast = document.createElement('div');
    toast.id = 'toastMsg';
    toast.style.cssText = 'position:fixed;bottom:60px;right:24px;background:#1a1a2e;border:1px solid #2a2a4a;' +
        'color:#e0e0e0;padding:10px 20px;border-radius:6px;font-size:13px;z-index:9999;' +
        'box-shadow:0 4px 12px rgba(0,0,0,0.3);max-width:400px;transition:opacity 0.3s;';
    toast.textContent = message;
    document.body.appendChild(toast);

    setTimeout(function() {
        toast.style.opacity = '0';
        setTimeout(function() { toast.remove(); }, 300);
    }, 3000);
}

// ============================================================
//  Utility Functions
// ============================================================

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
              .replace(/"/g, '&quot;').replace(/'/g, '&#039;');
}

function escapeAttr(str) {
    return str.replace(/'/g, "\\'").replace(/"/g, '\\"');
}

function cssId(str) {
    return str.replace(/[^a-zA-Z0-9]/g, '_');
}
