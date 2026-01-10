   let currentStep = 1;
    let selectedVendor = null;

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
        });
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
                    const fields = row.querySelectorAll('.editable-field');
                    monitorData.push({
                        name: fields[0].value,
                        type: fields[1].value,
                        query: fields[2].value,
                        threshold: fields[3].value
                    });
                });
                
                console.log('Monitors to migrate:', monitorData);
                return true;
                
            default:
                return true;
        }
    }

    function updateStepDisplay() {
        document.querySelectorAll('.step-content').forEach(step => {
            step.classList.remove('active');
        });
        
        document.getElementById(`step${currentStep}`).classList.add('active');
        
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
            4: { title: 'Review Monitors', subtitle: 'Select and edit monitors before migration' },
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