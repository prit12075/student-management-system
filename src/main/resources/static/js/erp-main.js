/**
 * Apex University ERP - Client-Side Interactive Utilities
 */

document.addEventListener('DOMContentLoaded', function () {
    // 1. Mobile sidebar toggle
    const toggleBtn = document.getElementById('sidebarToggleBtn');
    const sidebar = document.getElementById('erpSidebar');
    if (toggleBtn && sidebar) {
        toggleBtn.addEventListener('click', function () {
            sidebar.classList.toggle('show');
        });
    }

    // 2. Auto-calculate Total Marks and Grade preview on Faculty Marks page
    const markInputs = document.querySelectorAll('.mark-calc-input');
    markInputs.forEach(function (input) {
        input.addEventListener('input', function () {
            const studentId = this.getAttribute('data-student-id');
            if (!studentId) return;
            recalculateStudentRow(studentId);
        });
    });
});

function recalculateStudentRow(studentId) {
    const internalEl = document.querySelector(`input[name="internal_${studentId}"]`);
    const assignEl = document.querySelector(`input[name="assignment_${studentId}"]`);
    const practEl = document.querySelector(`input[name="practical_${studentId}"]`);
    const externEl = document.querySelector(`input[name="external_${studentId}"]`);

    const i = Math.min(30, Math.max(0, parseFloat(internalEl ? internalEl.value : 0) || 0));
    const a = Math.min(20, Math.max(0, parseFloat(assignEl ? assignEl.value : 0) || 0));
    const p = Math.min(20, Math.max(0, parseFloat(practEl ? practEl.value : 0) || 0));
    const e = Math.min(30, Math.max(0, parseFloat(externEl ? externEl.value : 0) || 0));

    const total = Math.round((i + a + p + e) * 10) / 10;
    let grade = 'F';
    let badgeClass = 'badge-soft-danger';

    if (total >= 90) { grade = 'O (PASS)'; badgeClass = 'badge-soft-success'; }
    else if (total >= 80) { grade = 'A+ (PASS)'; badgeClass = 'badge-soft-success'; }
    else if (total >= 70) { grade = 'A (PASS)'; badgeClass = 'badge-soft-success'; }
    else if (total >= 60) { grade = 'B+ (PASS)'; badgeClass = 'badge-soft-primary'; }
    else if (total >= 50) { grade = 'B (PASS)'; badgeClass = 'badge-soft-primary'; }
    else if (total >= 40) { grade = 'C (PASS)'; badgeClass = 'badge-soft-warning'; }
    else { grade = 'F (FAIL)'; badgeClass = 'badge-soft-danger'; }

    const totalCell = document.getElementById(`total_preview_${studentId}`);
    const gradeCell = document.getElementById(`grade_preview_${studentId}`);

    if (totalCell) totalCell.textContent = total.toFixed(1) + ' / 100';
    if (gradeCell) {
        gradeCell.className = 'badge rounded-pill px-3 py-2 ' + badgeClass;
        gradeCell.textContent = grade;
    }
}

function setAllAttendanceStatus(statusValue) {
    const radios = document.querySelectorAll(`input[type="radio"][value="${statusValue}"]`);
    radios.forEach(function (r) {
        r.checked = true;
    });
}

