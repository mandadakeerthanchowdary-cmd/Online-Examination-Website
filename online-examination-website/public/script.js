const views = ["loginView","instructionsView","examView","resultView"];
let student = {name:"", studentId:""};
let questions = [];
let remainingSeconds = 300;
let timerInterval = null;
let submitted = false;

function showView(id) {
  views.forEach(v => document.getElementById(v).classList.toggle("hidden", v !== id));
  window.scrollTo({top:0, behavior:"smooth"});
}

document.getElementById("loginForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  const name = document.getElementById("studentName").value.trim();
  const studentId = document.getElementById("studentId").value.trim();
  const error = document.getElementById("loginError");
  error.textContent = "";
  try {
    const response = await fetch("/api/login", {
      method:"POST", headers:{"Content-Type":"application/json"},
      body:JSON.stringify({name, studentId})
    });
    const data = await response.json();
    if (!response.ok) throw new Error(data.message || "Login failed.");
    student = {name:data.name, studentId:data.studentId};
    showView("instructionsView");
  } catch (e) {
    error.textContent = "Could not connect to the Java server. Make sure the server is running.";
  }
});

document.getElementById("startBtn").addEventListener("click", startExam);

async function startExam() {
  try {
    const response = await fetch("/api/questions");
    if (!response.ok) throw new Error("Unable to load questions.");
    const data = await response.json();
    questions = data.questions;
    renderQuestions();
    document.getElementById("studentWelcome").textContent =
      `Student: ${student.name} · ID: ${student.studentId}`;
    remainingSeconds = 300;
    submitted = false;
    updateTimer();
    showView("examView");
    timerInterval = setInterval(() => {
      remainingSeconds--;
      updateTimer();
      if (remainingSeconds <= 0) submitExam();
    }, 1000);
  } catch (e) {
    alert("Unable to load the exam. Please restart the Java server and try again.");
  }
}

function renderQuestions() {
  const container = document.getElementById("questionsContainer");
  container.innerHTML = questions.map((q, index) => `
    <article class="question-card">
      <div class="q-number">QUESTION ${index + 1} OF ${questions.length}</div>
      <h3>${escapeHtml(q.question)}</h3>
      ${q.options.map((option, optionIndex) => `
        <label class="option">
          <input type="radio" name="question-${q.id}" value="${optionIndex}">
          <span>${escapeHtml(option)}</span>
        </label>`).join("")}
    </article>`).join("");
  container.addEventListener("change", updateProgress);
  updateProgress();
}

function updateProgress() {
  const answered = questions.filter(q =>
    document.querySelector(`input[name="question-${q.id}"]:checked`)
  ).length;
  document.getElementById("progressText").textContent = `${answered} of ${questions.length} answered`;
  document.getElementById("progressBar").style.width = `${answered / questions.length * 100}%`;
}

function updateTimer() {
  const minutes = Math.floor(remainingSeconds / 60);
  const seconds = remainingSeconds % 60;
  document.getElementById("timer").textContent =
    `${String(minutes).padStart(2,"0")}:${String(seconds).padStart(2,"0")}`;
  document.querySelector(".timer-card").classList.toggle("warning", remainingSeconds <= 60);
}

document.getElementById("examForm").addEventListener("submit", (event) => {
  event.preventDefault();
  if (confirm("Are you sure you want to submit your examination?")) submitExam();
});

async function submitExam() {
  if (submitted) return;
  submitted = true;
  clearInterval(timerInterval);
  const answers = questions.map(q => {
    const selected = document.querySelector(`input[name="question-${q.id}"]:checked`);
    return selected ? Number(selected.value) : -1;
  });
  try {
    const response = await fetch("/api/submit", {
      method:"POST", headers:{"Content-Type":"application/json"},
      body:JSON.stringify({answers:answers.join(",")})
    });
    if (!response.ok) throw new Error("Result calculation failed.");
    const result = await response.json();
    showResult(result);
  } catch (e) {
    submitted = false;
    alert("Could not calculate the result. Please check the server connection.");
  }
}

function showResult(result) {
  document.getElementById("resultStudent").textContent =
    `${student.name} · Student ID: ${student.studentId}`;
  document.getElementById("score").textContent = `${result.score}/${result.total}`;
  document.getElementById("percentage").textContent = `${result.percentage}%`;
  document.getElementById("attempted").textContent = `${result.attempted}/${result.total}`;
  document.getElementById("correct").textContent = result.score;
  document.getElementById("resultMessage").textContent =
    result.percentage >= 80 ? "Excellent work! You have performed very well." :
    result.percentage >= 50 ? "Good effort! Keep practising to improve your score." :
    "Keep learning and practising. You can do better next time.";
  showView("resultView");
}

document.getElementById("restartBtn").addEventListener("click", () => {
  clearInterval(timerInterval);
  document.getElementById("loginForm").reset();
  document.getElementById("loginError").textContent = "";
  showView("loginView");
});

function escapeHtml(value) {
  return String(value).replace(/[&<>"']/g, char =>
    ({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[char]));
}
