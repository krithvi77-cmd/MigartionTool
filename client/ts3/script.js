let currentStep = 1;
    let selectedVendor = null;
    let monitors = []; // Store monitor data

    const modal = document.getElementById('migrationModal');
    const startBtn = document.getElementById('startMigrationBtn');
    const closeBtn = document.getElementById('closeModal');
    const nextBtn = document.getElementById('nextBtn');
    const backBtn = document.getElementById('backBtn');
    const finishBtn = document.getElementById('finishBtn');
    const selectAllCheckbox = document.getElementById('selectAll');

    startBtn.addEventListener('click', () => {
        modal.classList.add('active');
        currentStep = 1;
        updateStepDisplay();
    });

    closeBtn.addEventListener('click', () => {
        modal.classList.remove('active');
    });

    modal.addEventListener('click', (e) => {
        if (e.target === modal) {
            modal.classList.remove('active');
        }
    });

    document.querySelectorAll('.vendor-card:not(.disabled)').forEach(card => {
        card.addEventListener('click', () => {
            document.querySelectorAll('.vendor-card').forEach(c => c.classList.remove('selected'));
            card.classList.add('selected');
            selectedVendor = card.dataset.vendor;
        });
    });

    selectAllCheckbox.addEventListener('change', (e) => {
        document.querySelectorAll('.monitor-select').forEach(checkbox => {
            checkbox.checked = e.target.checked;
            const row = checkbox.closest('tr');
            if (row) {
                if (e.target.checked) {
                    row.classList.add('selected');
                } else {
                    row.classList.remove('selected');
                }
            }
        });
        updateMonitorCount();
    });

    nextBtn.addEventListener('click', () => {
        if (validateStep(currentStep)) {
            if (currentStep < 5) {
                currentStep++;
                updateStepDisplay();
            }
            
            if (currentStep === 5) {
                console.log('Starting migration...');
            }
        }
    });

    backBtn.addEventListener('click', () => {
        if (currentStep > 1) {
            currentStep--;
            updateStepDisplay();
        }
    });

    finishBtn.addEventListener('click', () => {
        modal.classList.remove('active');
        currentStep = 1;
        updateStepDisplay();
    });

    function validateStep(step) {
        switch(step) {
            case 1:
                const clientId = document.getElementById('clientId').value;
                const clientSecret = document.getElementById('clientSecret').value;
                const readToken = document.getElementById('readRefreshToken').value;
                const createToken = document.getElementById('createRefreshToken').value;
                
                if (!clientId || !clientSecret || !readToken || !createToken) {
                    alert('Please fill all Site24x7 credentials');
                    return false;
                }
                return true;
                
            case 2:
                if (!selectedVendor) {
                    alert('Please select a vendor');
                    return false;
                }
                return true;
                
            case 3:
                const apiKey = document.getElementById('datadogApiKey').value;
                const appKey = document.getElementById('datadogAppKey').value;
                
                if (!apiKey || !appKey) {
                    alert('Please fill all vendor credentials');
                    return false;
                }
                
                console.log('Fetching monitors from', selectedVendor);
               
                loadSampleMonitors();
                return true;
                
            case 4:
                const selectedMonitors = document.querySelectorAll('.monitor-select:checked');
                if (selectedMonitors.length === 0) {
                    alert('Please select at least one monitor to migrate');
                    return false;
                }
                
                const monitorData = [];
                selectedMonitors.forEach(checkbox => {
                    const row = checkbox.closest('tr');
                    const monitorId = row.dataset.monitorId;
                    const monitor = monitors.find(m => m.id === monitorId);
                    
                    if (monitor) {
                        const updatedMonitor = { ...monitor };
                        row.querySelectorAll('.editable-cell input, .editable-cell textarea').forEach(field => {
                            const fieldName = field.dataset.field;
                            updatedMonitor[fieldName] = field.value;
                        });
                        monitorData.push(updatedMonitor);
                    }
                });
                
                console.log('Monitors to migrate:', monitorData);
                return true;
                
            default:
                return true;
        }
    }

    function loadSampleMonitors() {
        
        monitors = [
            {
                id: 'mon_1',
                name: 'CPU Usage Alert',
                type: 'metric alert',
                query: 'avg(last_5m):avg:system.cpu.user{*} by {host}',
                threshold: '80',
                warningThreshold: '70',
                message: 'CPU usage is high on {{host.name}}',
                tags: 'env:production, team:platform',
                evaluationWindow: '5m',
                notifyNoData: 'true',
                renotifyInterval: '60',
                escalationMessage: 'CPU still high after 1 hour',
                priority: 'P2'
            },
            {
                id: 'mon_2',
                name: 'Memory Usage Critical',
                type: 'metric alert',
                query: 'avg(last_10m):avg:system.mem.used{*} / avg:system.mem.total{*}',
                threshold: '90',
                warningThreshold: '80',
                message: 'Memory usage critical on {{host.name}}',
                tags: 'env:production, team:platform',
                evaluationWindow: '10m',
                notifyNoData: 'false',
                renotifyInterval: '30',
                escalationMessage: 'Memory issue persists',
                priority: 'P1'
            },
            {
                id: 'mon_3',
                name: 'Disk Space Warning',
                type: 'metric alert',
                query: 'avg(last_15m):avg:system.disk.used{*} / avg:system.disk.total{*}',
                threshold: '85',
                warningThreshold: '75',
                message: 'Disk space running low on {{host.name}}',
                tags: 'env:production, team:infrastructure',
                evaluationWindow: '15m',
                notifyNoData: 'true',
                renotifyInterval: '120',
                escalationMessage: 'Disk space critical',
                priority: 'P3'
            },
            {
                id: 'mon_4',
                name: 'API Response Time',
                type: 'apm alert',
                query: 'avg(last_5m):avg:trace.web.request.duration{service:api}',
                threshold: '500',
                warningThreshold: '300',
                message: 'API response time is degraded',
                tags: 'env:production, team:backend, service:api',
                evaluationWindow: '5m',
                notifyNoData: 'true',
                renotifyInterval: '15',
                escalationMessage: 'API performance still degraded',
                priority: 'P1'
            },
            {
                id: 'mon_5',
                name: 'Error Rate Anomaly',
                type: 'anomaly alert',
                query: 'avg(last_1h):anomalies(avg:trace.web.request.errors{*}, "basic", 2)',
                threshold: '10',
                warningThreshold: '5',
                message: 'Unusual error rate detected',
                tags: 'env:production, team:backend',
                evaluationWindow: '1h',
                notifyNoData: 'false',
                renotifyInterval: '60',
                escalationMessage: 'Error rate anomaly continues',
                priority: 'P2',
                seasonality: 'weekly'
            },
            {
                id: 'mon_6',
                name: 'Database Connection Pool',
                type: 'metric alert',
                query: 'avg(last_5m):avg:postgresql.connections.used{*} / avg:postgresql.connections.max{*}',
                threshold: '85',
                warningThreshold: '70',
                message: 'Database connection pool usage high',
                tags: 'env:production, team:database, service:postgresql',
                evaluationWindow: '5m',
                notifyNoData: 'true',
                renotifyInterval: '45',
                escalationMessage: 'Connection pool saturated',
                priority: 'P1'
            }
        ];
        
        renderMonitors();
    }

    function renderMonitors() {
        const tableWrapper = document.getElementById('monitorsTableWrapper');
        
        if (monitors.length === 0) {
            tableWrapper.innerHTML = `
                <div class="no-monitors">
                    <div class="no-monitors-icon">Micon</div>
                    <h3>No Monitors Found</h3>
                    <p>No monitors were found in your source platform</p>
                </div>
            `;
            return;
        }
        
        // Get all unique fields from all monitors
        const allFields = new Set();
        monitors.forEach(monitor => {
            Object.keys(monitor).forEach(key => {
                if (key !== 'id') allFields.add(key);
            });
        });
        
        const fields = Array.from(allFields);
        
        // Create table
        let tableHTML = `
            <table class="monitors-table">
                <thead>
                    <tr>
                        <th>
                            <input type="checkbox" class="monitor-checkbox" id="tableSelectAll">
                        </th>
                        ${fields.map(field => `
                            <th>${formatFieldName(field)}</th>
                        `).join('')}
                    </tr>
                </thead>
                <tbody>
                    ${monitors.map((monitor, index) => `
                        <tr data-monitor-id="${monitor.id}" class="selected">
                            <td>
                                <input type="checkbox" class="monitor-checkbox monitor-select" checked onchange="handleMonitorRowSelect(this)">
                            </td>
                            ${fields.map(field => {
                                const value = monitor[field] || '';
                                const isLongText = typeof value === 'string' && value.length > 50;
                                
                                if (field === 'name') {
                                    return `
                                        <td class="editable-cell monitor-name-cell">
                                            <input type="text" value="${value}" data-field="${field}">
                                        </td>
                                    `;
                                } else if (field === 'type') {
                                    return `
                                        <td>
                                            <span class="type-badge">${value}</span>
                                        </td>
                                    `;
                                } else if (isLongText) {
                                    return `
                                        <td class="editable-cell">
                                            <textarea data-field="${field}" rows="3">${value}</textarea>
                                        </td>
                                    `;
                                } else {
                                    return `
                                        <td class="editable-cell">
                                            <input type="text" value="${value}" data-field="${field}">
                                        </td>
                                    `;
                                }
                            }).join('')}
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        `;
        
        tableWrapper.innerHTML = tableHTML;
    
        document.getElementById('tableSelectAll').addEventListener('change', (e) => {
            document.querySelectorAll('.monitor-select').forEach(checkbox => {
                checkbox.checked = e.target.checked;
                const row = checkbox.closest('tr');
                if (row) {
                    if (e.target.checked) {
                        row.classList.add('selected');
                    } else {
                        row.classList.remove('selected');
                    }
                }
            });
            updateMonitorCount();
        });
        
        updateMonitorCount();
    }

    function formatFieldName(fieldName) {
        return fieldName
            .replace(/([A-Z])/g, ' $1')
            .replace(/^./, str => str.toUpperCase())
            .trim();
    }

    function updateMonitorCount() {
        const selected = document.querySelectorAll('.monitor-select:checked').length;
        const total = monitors.length;
        
        document.getElementById('selectedCount').textContent = selected;
        document.getElementById('totalCount').textContent = total;
        
        const selectAll = document.getElementById('selectAll');
        if (selectAll) {
            selectAll.checked = selected === total && total > 0;
            selectAll.indeterminate = selected > 0 && selected < total;
        }
    }

    // Global functions for onclick handlers
    window.handleMonitorRowSelect = function(checkbox) {
        const row = checkbox.closest('tr');
        if (checkbox.checked) {
            row.classList.add('selected');
        } else {
            row.classList.remove('selected');
        }
        updateMonitorCount();
    };

    function updateStepDisplay() {
        document.querySelectorAll('.step-content').forEach(step => {
            step.classList.remove('active');
        });
        
        document.getElementById(`step${currentStep}`).classList.add('active');
        
        if (currentStep === 4) {
            modal.classList.add('fullscreen');
        } else {
            modal.classList.remove('fullscreen');
        }
        
        const progressSteps = document.querySelectorAll('.progress-step');
        progressSteps.forEach((step, index) => {
            step.classList.remove('active', 'completed');
            if (index + 1 < currentStep) {
                step.classList.add('completed');
            } else if (index + 1 === currentStep) {
                step.classList.add('active');
            }
        });
        
        const progressPercent = ((currentStep - 1) / 4) * 100;
        document.getElementById('progressFill').style.width = progressPercent + '%';
        
        const titles = {
            1: { title: 'Site24x7 Credentials', subtitle: 'Enter your Site24x7 API credentials to begin migration' },
            2: { title: 'Select Source Vendor', subtitle: 'Choose the platform you want to migrate from' },
            3: { title: 'Vendor Credentials', subtitle: 'Enter your ' + (selectedVendor || 'vendor') + ' credentials' },
            4: { title: 'Review & Edit Monitors', subtitle: 'Select monitors and edit their configuration before migration' },
            5: { title: 'Migration Complete', subtitle: 'Your monitors are being migrated' }
        };
        
        document.getElementById('modalTitle').textContent = titles[currentStep].title;
        document.getElementById('modalSubtitle').textContent = titles[currentStep].subtitle;
        
        if (currentStep === 5) {
            nextBtn.style.display = 'none';
            backBtn.style.display = 'none';
        } else {
            nextBtn.style.display = 'block';
            backBtn.style.display = currentStep === 1 ? 'none' : 'block';
        }
    }

    updateStepDisplay();