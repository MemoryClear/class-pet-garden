<template>
  <div class="classroom-container">
    <div class="classroom-header">
      <h1>📖 课堂</h1>
      <button class="back-btn" @click="$router.push('/')">← 返回</button>
    </div>
    <div class="tabs">
      <button v-for="tab in ['语文', '数学', '英语']" :key="tab" :class="['tab-btn', { active: activeTab === tab }]" @click="activeTab = tab">{{ tab }}</button>
    </div>

    <!-- 语文 -->
    <div v-if="activeTab === '语文'" class="tab-content">
      <div class="chinese-sub-tabs">
        <button :class="['sub-tab-btn', { active: chineseMode === 'poems' }]" @click="chineseMode = 'poems'">📜 诗词</button>
        <button :class="['sub-tab-btn', { active: chineseMode === 'pinyin' }]" @click="chineseMode = 'pinyin'">🔤 拼音</button>
      </div>

      <!-- 诗词模式 -->
      <div v-if="chineseMode === 'poems'">
        <div class="poem-toolbar">
          <div class="poem-stats">
            <span>共 {{ poems.length }} 首</span>
            <span class="learned-count">已学习 {{ learnedCount }} 首 ✅</span>
          </div>
          <div class="poem-actions">
            <button class="add-poem-btn" @click="showAddPoemModal = true">➕ 添加诗词</button>
            <button v-if="isStudent" class="add-poem-btn" @click="startPoemQuiz">📝 诗词测验</button>
            <div class="poem-filter">
              <button :class="['filter-btn', { active: poemFilter === 'all' }]" @click="poemFilter = 'all'">全部</button>
              <button :class="['filter-btn', { active: poemFilter === 'learned' }]" @click="poemFilter = 'learned'">已学习</button>
              <button :class="['filter-btn', { active: poemFilter === 'unlearned' }]" @click="poemFilter = 'unlearned'">未学习</button>
            </div>
          </div>
        </div>
        <div v-if="loading" class="loading">加载中...</div>
        <div v-else-if="poems.length === 0" class="empty">暂无诗词，点击上方按钮添加</div>
        <div v-else-if="!poemQuizActive" class="poem-list">
          <div
            v-for="(poem, index) in filteredPoems"
            :key="poem.title + poem.author"
            class="poem-card"
            :class="{ learned: isLearned(poem) }"
            @click="showPoem(poem)"
          >
            <div class="poem-card-header">
              <h3>{{ poem.title }}</h3>
              <button
                class="learn-btn"
                :class="{ 'is-learned': isLearned(poem) }"
                @click.stop="toggleLearned(poem)"
                :title="isLearned(poem) ? '取消学习标记' : '标记为已学习'"
              >
                {{ isLearned(poem) ? '✅' : '☐' }}
              </button>
            </div>
            <p class="author">{{ poem.author }}<span v-if="poem.dynasty" class="dynasty">（{{ poem.dynasty }}）</span></p>
            <p class="preview">{{ poem.content.split('\n')[0] }}...</p>
          </div>
        </div>
        <!-- 诗词详情弹窗 -->
        <div v-if="selectedPoem" class="modal-overlay" @click="selectedPoem = null">
          <div class="modal" @click.stop>
            <div class="modal-header">
              <h2>{{ selectedPoem.title }}</h2>
              <div class="modal-header-actions">
                <button
                  class="learn-btn-lg"
                  :class="{ 'is-learned': isLearned(selectedPoem) }"
                  @click="toggleLearned(selectedPoem)"
                >
                  {{ isLearned(selectedPoem) ? '✅ 已学习' : '☐ 标记学习' }}
                </button>
                <button class="close-btn" @click="selectedPoem = null">×</button>
              </div>
            </div>
            <p class="modal-author">{{ selectedPoem.author }}<span v-if="selectedPoem.dynasty" class="dynasty">（{{ selectedPoem.dynasty }}）</span></p>
            <pre class="modal-content">{{ selectedPoem.content }}</pre>
            <div v-if="showPinyin" class="modal-pinyin">{{ getPinyin(selectedPoem.content) }}</div>
            <div class="modal-actions">
              <button @click="speakPoem(selectedPoem)">🔊 朗读</button>
              <button @click="startDictation">✏️ 默写</button>
              <button @click="showPinyin = !showPinyin" :class="{ active: showPinyin }">📝 拼音</button>
              <button class="delete-btn" @click="deletePoem(selectedPoem)">🗑️ 删除</button>
            </div>
            <div v-if="dictationMode" class="dictation-area">
              <p>请默写：{{ selectedPoem.title }}</p>
              <textarea v-model="userInput" placeholder="在这里默写..."></textarea>
              <button @click="checkDictation">检查</button>
              <p :class="dictationResult === '正确！🎉' ? 'correct' : 'wrong'">{{ dictationResult }}</p>
            </div>
          </div>
        </div>
        <!-- 添加诗词弹窗 -->
        <div v-if="showAddPoemModal" class="modal-overlay" @click="showAddPoemModal = false">
          <div class="modal add-poem-modal" @click.stop>
            <div class="modal-header">
              <h2>➕ 添加诗词</h2>
              <button class="close-btn" @click="showAddPoemModal = false">×</button>
            </div>
            <div class="add-poem-form">
              <label>标题：<input v-model="newPoem.title" placeholder="如：静夜思"></label>
              <label>作者：<input v-model="newPoem.author" placeholder="如：李白"></label>
              <label>朝代：<input v-model="newPoem.dynasty" placeholder="如：唐"></label>
              <label>内容：<textarea v-model="newPoem.content" placeholder="每句一行，用回车分隔"></textarea></label>
              <button class="submit-btn" @click="addPoem" :disabled="!newPoem.title || !newPoem.author || !newPoem.content">添加</button>
              <p v-if="addError" class="error">{{ addError }}</p>
            </div>
          </div>
        </div>
        <div v-if="poemQuizActive" class="poem-quiz-section">
          <div class="quiz-header-bar">
            <span class="quiz-progress">{{ poemQuizIndex + 1 }} / {{ poemQuizBlanks.length }}</span>
            <span class="quiz-score-display">得分: {{ poemQuizScore }} 分</span>
          </div>
          <div v-if="poemQuizIndex < poemQuizBlanks.length" class="poem-quiz-card">
            <p class="poem-quiz-title">{{ poemQuizBlanks[poemQuizIndex].poem.title }} - {{ poemQuizBlanks[poemQuizIndex].poem.author }}</p>
            <div class="poem-quiz-content">
              <span
                v-for="(char, ci) in poemQuizBlanks[poemQuizIndex].poem.content.split('').filter(c => c !== '\n')"
                :key="ci"
              >
                <span
                  v-if="poemQuizBlanks[poemQuizIndex].blankPositions && poemQuizBlanks[poemQuizIndex].blankPositions.includes(ci)"
                  class="poem-blank"
                  :class="{ revealed: poemQuizChecked }"
                >
                  {{ poemQuizChecked ? char : '_' }}
                </span>
                <span v-else>{{ char }}</span>
              </span>
            </div>
            <div class="poem-blanks-input" v-if="!poemQuizChecked">
              <p class="poem-blanks-hint">请填写所有下划线处的汉字：</p>
              <div class="poem-blanks-row">
                <input
                  v-for="(blank, bi) in poemQuizBlanks[poemQuizIndex].blanks"
                  :key="bi"
                  v-model="poemQuizAnswers[bi]"
                  class="poem-blank-input"
                  maxlength="1"
                  placeholder="字"
                />
              </div>
              <button class="submit-quiz-btn" @click="checkPoemBlanks">确定</button>
            </div>
            <div v-if="poemQuizChecked" class="quiz-feedback">
              <p v-for="(blank, bi) in poemQuizBlanks[poemQuizIndex].blanks" :key="bi">
                {{ blank.char === poemQuizAnswers[bi] ? '✓ 正确：' + blank.char : '✗ 正确：' + blank.char }}
              </p>
            </div>
            <button
              v-if="poemQuizChecked && poemQuizIndex < poemQuizBlanks.length - 1"
              class="next-quiz-btn"
              @click="nextPoemQuestion"
            >下一题</button>
            <button
              v-if="poemQuizChecked && poemQuizIndex >= poemQuizBlanks.length - 1"
              class="next-quiz-btn"
              @click="poemQuizActive = false"
            >完成！</button>
          </div>
          <div v-else class="quiz-finish">
            <p class="finish-score">测验完成！得分：{{ poemQuizScore }} / {{ poemQuizBlanks.length * poemQuizBlanks[0].blanks.length }}</p>
            <button class="gen-btn" @click="startPoemQuiz">再来一组</button>
            <button class="gen-btn secondary" @click="poemQuizActive = false">返回诗词</button>
          </div>
        </div>
      </div>
      <!-- /诗词模式 -->

      <!-- 拼音学习模式 -->
      <div v-if="chineseMode === 'pinyin'" class="pinyin-section">
        <div class="pinyin-toggle">
          <button :class="['toggle-btn', { active: pinyinType === 'shengmu' }]" @click="pinyinType = 'shengmu'">声母 (23)</button>
          <button :class="['toggle-btn', { active: pinyinType === 'yunmu' }]" @click="pinyinType = 'yunmu'">韵母 (24)</button>
          <button :class="['toggle-btn', { active: pinyinType === 'tone' }]" @click="pinyinType = 'tone'">🎵 声调</button>
        </div>
        <div v-if="pinyinType === 'tone'" class="tone-section">
          <p class="tone-intro">汉语有四个声调，配合声母韵母构成完整的音节</p>
          <div class="tone-cards">
            <div v-for="t in toneList" :key="t.tone" class="tone-card" :class="'tone-' + t.tone">
              <div class="tone-icon">{{ t.icon }}</div>
              <div class="tone-number">第{{ t.tone }}声</div>
              <div class="tone-mark">{{ t.mark }}</div>
              <div class="tone-name">{{ t.name }}</div>
              <div class="tone-desc">{{ t.desc }}</div>
              <div class="tone-example">{{ t.example }}</div>
              <button class="tone-speak-btn" @click="speakTone(t)">🔊 发音</button>
            </div>
          </div>
        </div>
        <div v-if="pinyinType !== 'tone'" class="pinyin-cards">
          <div
            v-for="item in (pinyinType === 'shengmu' ? shengmuList : yunmuList)"
            :key="item.letter"
            class="pinyin-card"
            :class="{ flipped: item.flipped }"
            @click="item.flipped = !item.flipped"
          >
            <div class="pinyin-front">
              {{ item.letter }}
              <button class="speak-btn" @click.stop="speakPinyin(item)" title="点击发声">🔊</button>
            </div>
            <div class="pinyin-back">
              <div class="pinyin-sound">{{ item.sound }}</div>
              <div class="pinyin-example">{{ item.example }}</div>
            </div>
          </div>
        </div>
        <p v-if="pinyinType !== 'tone'" class="hint">点击卡片翻转 · 点击 🔊 发音</p>
      </div>
      <!-- /拼音学习模式 -->
    </div>
    <!-- /语文 -->

    <!-- 数学 -->
    <div v-if="activeTab === '数学'" class="tab-content">
      <div class="math-section-toggle">
        <button :class="['toggle-btn', { active: mathMode === 'arithmetic' }]" @click="mathMode = 'arithmetic'">🔢 四则运算</button>
        <button :class="['toggle-btn', { active: mathMode === 'multiply' }]" @click="mathMode = 'multiply'">✖️ 九九乘法表</button>
      </div>

      <div v-if="mathMode === 'arithmetic'">
        <div class="math-config">
          <div class="config-row">
            <label>最大数字：<input type="number" v-model.number="mathConfig.maxNum" min="1" max="1000"></label>
            <label>题目数量：<input type="number" v-model.number="mathConfig.count" min="1" max="50"></label>
          </div>
          <div class="checkbox-group">
            <label><input type="checkbox" value="+" v-model="mathConfig.operations"> +</label>
            <label><input type="checkbox" value="-" v-model="mathConfig.operations"> -</label>
            <label><input type="checkbox" value="×" v-model="mathConfig.operations"> ×</label>
            <label><input type="checkbox" value="÷" v-model="mathConfig.operations"> ÷</label>
          </div>
          <button class="gen-btn" @click="generateMathProblems">生成题目</button>
        </div>
        <div v-if="mathProblems.length" class="math-quiz-list">
          <div v-for="(p, i) in mathProblems" :key="i" class="math-quiz-item" :class="{ answered: p.result !== null }">
            <div class="quiz-question">
              <span class="quiz-index">{{ i + 1 }}.</span>
              <span class="quiz-text">{{ p.question }}</span>
              <button v-if="p.result === null" class="peek-btn" @click="peekAnswer(i)" :title="p.showAnswer ? '隐藏答案' : '查看答案'">{{ p.showAnswer ? '🙈' : '👁️' }}</button>
            </div>
            <div v-if="p.showAnswer && p.result === null" class="peek-answer">答案：{{ p.answer }}</div>
            <div v-if="p.result === null && !p.showAnswer" class="quiz-input">
              <input type="number" v-model.number="p.userAnswer" placeholder="?" @keyup.enter="checkOneProblem(i)">
              <button @click="checkOneProblem(i)">提交</button>
            </div>
            <div v-if="p.result !== null" :class="p.result ? 'correct' : 'wrong'">
              {{ p.result ? '✓ 正确！' : '✗答案是 ' + p.answer + '（你答：' + p.userAnswer + '）' }}
            </div>
          </div>
        </div>
        <div class="quiz-summary" v-if="mathProblems.length && mathProblems.every(p => p.result !== null)">
          <p>🎉 全部完成！正确 {{ mathProblems.filter(p => p.result).length }} / {{ mathProblems.length }}</p>
          <div class="quiz-actions">
            <button v-if="isStudent && !mathSubmitted" class="submit-score-btn" @click="submitMathScore">🏆 交卷得分</button>
            <span v-if="mathSubmitted" class="score-submitted">✅ 已交卷 +{{ mathSubmittedScore }}分</span>
            <button class="gen-btn" @click="generateMathProblems">再来一组</button>
          </div>
        </div>
        <div class="wrong-records" v-if="wrongRecords.length">
          <h3>错题记录</h3>
          <ul><li v-for="(r, i) in wrongRecords" :key="i">{{ r }}</li></ul>
          <button @click="wrongRecords = []">清空</button>
        </div>
      </div>

      <div v-if="mathMode === 'multiply'" class="multiply-section">
        <div class="multiply-toolbar">
          <button :class="['toggle-btn', { active: multiplyMode === 'table' }]" @click="multiplyMode = 'table'">📊 查看表格</button>
          <button :class="['toggle-btn', { active: multiplyMode === 'quiz' }]" @click="multiplyMode = 'quiz'">✏️ 练习</button>
        </div>
        <div v-if="multiplyMode === 'table'" class="multiply-table-wrap">
          <table class="multiply-table">
            <thead>
              <tr><th>×</th><th v-for="j in 9" :key="j">{{ j }}</th></tr>
            </thead>
            <tbody>
              <tr v-for="i in 9" :key="i">
                <th>{{ i }}</th>
                <td v-for="j in 9" :key="j" :class="{ highlight: i >= j }">{{ i }}×{{ j }}={{ i * j }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-if="multiplyMode === 'quiz'">
          <div class="multiply-quiz-config">
            <label>题目数量：<input type="number" v-model.number="multiplyConfig.count" min="1" max="50"></label>
            <button class="gen-btn" @click="generateMultiplyQuiz">开始练习</button>
          </div>
          <div v-if="multiplyProblems.length" class="math-quiz-list">
            <div v-for="(p, i) in multiplyProblems" :key="i" class="math-quiz-item" :class="{ answered: p.result !== null }">
              <div class="quiz-question">
                <span class="quiz-index">{{ i + 1 }}.</span>
                <span class="quiz-text">{{ p.question }}</span>
                <button v-if="p.result === null" class="peek-btn" @click="p.showAnswer = !p.showAnswer" :title="p.showAnswer ? '隐藏答案' : '查看答案'">{{ p.showAnswer ? '🙈' : '👁️' }}</button>
              </div>
              <div v-if="p.showAnswer && p.result === null" class="peek-answer">答案：{{ p.answer }}</div>
              <div v-if="p.result === null && !p.showAnswer" class="quiz-input">
                <input type="number" v-model.number="p.userAnswer" placeholder="?" @keyup.enter="checkMultiplyOne(i)">
                <button @click="checkMultiplyOne(i)">提交</button>
              </div>
              <div v-if="p.result !== null" :class="p.result ? 'correct' : 'wrong'">
                {{ p.result ? '✓ 正确！' : '✗ 答案是 ' + p.answer + '（你答：' + p.userAnswer + '）' }}
              </div>
            </div>
            <div class="quiz-summary" v-if="multiplyProblems.every(p => p.result !== null)">
              <p>🎉 全部完成！正确 {{ multiplyProblems.filter(p => p.result).length }} / {{ multiplyProblems.length }}</p>
              <div class="quiz-actions">
                <button v-if="isStudent && !multiplySubmitted" class="submit-score-btn" @click="submitMultiplyScore">🏆 交卷得分</button>
                <span v-if="multiplySubmitted" class="score-submitted">✅ 已交卷 +{{ multiplySubmittedScore }}分</span>
                <button class="gen-btn" @click="generateMultiplyQuiz">再来一组</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    <!-- /数学 -->

    <!-- 英语 -->
    <div v-if="activeTab === '英语'" class="tab-content">
      <div class="english-toolbar">
        <button :class="['speak-all-btn', { active: englishMode === 'cards' }]" @click="englishMode = 'cards'">🔤 字母</button>
        <button class="speak-all-btn" @click="speakAllLetters">🔊 朗读全部</button>
        <button :class="['speak-all-btn', { active: englishMode === 'quiz' }]" @click="startEnglishQuiz">📝 测验</button>
      </div>

      <!-- 英语字母卡片模式 -->
      <div v-if="englishMode === 'cards'" class="english-cards">
        <div v-for="letter in letters" :key="letter.upper" class="letter-card" :class="{ flipped: letter.flipped }" @click="letter.flipped = !letter.flipped">
          <div class="card-front">
            <span class="letter-char">{{ letter.lower }}</span>
            <button class="card-speak-btn" @click.stop="speakLetter(letter.lower)">🔊</button>
          </div>
          <div class="card-back">
            <span class="letter-char">{{ letter.upper }}</span>
          </div>
        </div>
      </div>

      <!-- 英语字母测验模式 -->
      <div v-if="englishMode === 'quiz' && englishQuizActive" class="english-quiz-section">
        <div class="quiz-header-bar">
          <span class="quiz-progress">{{ englishQuizIndex + 1 }} / {{ englishQuizQuestions.length }}</span>
          <span class="quiz-score-display">得分: {{ englishQuizScore }} 分</span>
        </div>
        <div v-if="englishQuizIndex < englishQuizQuestions.length" class="english-quiz-card">
          <div class="quiz-target-letter">
            <span class="target-char">{{ englishQuizQuestions[englishQuizIndex].letter.lower }}</span>
            <button class="speak-btn" @click="speakLetter(englishQuizQuestions[englishQuizIndex].letter.lower)">🔊 发音</button>
          </div>
          <p class="quiz-prompt">请问这个字母的大写是什么？</p>
          <div class="quiz-choices">
            <button
              v-for="(choice, ci) in englishQuizQuestions[englishQuizIndex].options"
              :key="ci"
              :class="['quiz-choice-btn', {
                selected: englishQuizSelected === choice,
                correct: englishQuizSubmitted && choice === englishQuizQuestions[englishQuizIndex].letter.upper,
                wrong: englishQuizSubmitted && englishQuizSelected === choice && choice !== englishQuizQuestions[englishQuizIndex].letter.upper
              }]"
              :disabled="englishQuizSubmitted"
              @click="englishQuizSelected = choice"
            >{{ choice }}</button>
          </div>
          <div v-if="englishQuizSubmitted" class="quiz-feedback">
            {{ englishQuizSelected === englishQuizQuestions[englishQuizIndex].letter.upper ? '✓ 正确！+1分' : '✗ 正确答案是 ' + englishQuizQuestions[englishQuizIndex].letter.upper }}
          </div>
          <button v-if="!englishQuizSubmitted" class="submit-quiz-btn" :disabled="englishQuizSelected === null" @click="submitEnglishAnswer">确定</button>
          <button v-if="englishQuizSubmitted && englishQuizIndex < englishQuizQuestions.length - 1" class="next-quiz-btn" @click="nextEnglishQuestion">下一题 →</button>
          <button v-if="englishQuizSubmitted && englishQuizIndex >= englishQuizQuestions.length - 1" class="next-quiz-btn" @click="englishQuizActive = false; englishMode = 'cards'">完成</button>
        </div>
        <div v-if="englishQuizIndex >= englishQuizQuestions.length && englishQuizActive" class="quiz-finish">
          <p class="finish-score">🎉 测验完成！得分：{{ englishQuizScore }} / {{ englishQuizQuestions.length }}</p>
          <button class="gen-btn" @click="startEnglishQuiz">再来一组</button>
          <button class="gen-btn secondary" @click="englishQuizActive = false; englishMode = 'cards'">返回</button>
        </div>
      </div>
      <div v-if="englishMode === 'quiz' && !englishQuizActive" class="english-quiz-placeholder">
        <p>点击上方「测验」按钮开始英语字母测验</p>
      </div>
    </div>
    <!-- /英语 -->
  </div>
</template>
<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { classroomApi, studentApi2 } from '../api/index.js'
import { useAuthStore } from '../stores/auth.js'
import { pinyin as py } from 'pinyin'

// 拼音辅助函数：将 pinyin 数组转为空格分隔的字符串
function toPinyin(text) {
  if (!text) return ''
  const lines = text.split('\n')
  return lines.map(line => {
    const words = py(line, { style: py.STYLE_NORMAL })
    return words.map(w => w[0]).join(' ')
  }).join('\n')
}

const activeTab = ref('语文')
const poemFilter = ref('all')
const loading = ref(false)

// 语文子模式
const chineseMode = ref('poems')
const pinyinType = ref('shengmu')

// 声母数据（23个）
const shengmuList = ref([
  { letter: 'b', sound: 'bō', example: '爸 (bà)', flipped: false },
  { letter: 'p', sound: 'pō', example: '怕 (pà)', flipped: false },
  { letter: 'm', sound: 'mō', example: '妈 (mā)', flipped: false },
  { letter: 'f', sound: 'fō', example: '发 (fā)', flipped: false },
  { letter: 'd', sound: 'dē', example: '大 (dà)', flipped: false },
  { letter: 't', sound: 'tē', example: '他 (tā)', flipped: false },
  { letter: 'n', sound: 'nē', example: '你 (nǐ)', flipped: false },
  { letter: 'l', sound: 'lē', example: '拉 (lā)', flipped: false },
  { letter: 'g', sound: 'gē', example: '哥 (gē)', flipped: false },
  { letter: 'k', sound: 'kē', example: '可 (kě)', flipped: false },
  { letter: 'h', sound: 'hē', example: '和 (hé)', flipped: false },
  { letter: 'j', sound: 'jī', example: '机 (jī)', flipped: false },
  { letter: 'q', sound: 'qī', example: '七 (qī)', flipped: false },
  { letter: 'x', sound: 'xī', example: '西 (xī)', flipped: false },
  { letter: 'zh', sound: 'zhī', example: '知 (zhī)', flipped: false },
  { letter: 'ch', sound: 'chī', example: '吃 (chī)', flipped: false },
  { letter: 'sh', sound: 'shī', example: '是 (shì)', flipped: false },
  { letter: 'r', sound: 'rì', example: '日 (rì)', flipped: false },
  { letter: 'z', sound: 'zī', example: '字 (zì)', flipped: false },
  { letter: 'c', sound: 'cī', example: '次 (cì)', flipped: false },
  { letter: 's', sound: 'sī', example: '四 (sì)', flipped: false },
  { letter: 'y', sound: 'yī', example: '一 (yī)', flipped: false },
  { letter: 'w', sound: 'wū', example: '五 (wǔ)', flipped: false }
])

// 韵母数据（24个）
const yunmuList = ref([
  { letter: 'a', sound: 'ā', example: '大 (dà)', flipped: false },
  { letter: 'o', sound: 'ō', example: '我 (wǒ)', flipped: false },
  { letter: 'e', sound: 'ē', example: '鹅 (é)', flipped: false },
  { letter: 'i', sound: 'ī', example: '你 (nǐ)', flipped: false },
  { letter: 'u', sound: 'ū', example: '五 (wǔ)', flipped: false },
  { letter: 'ü', sound: 'ǖ', example: '鱼 (yú)', flipped: false },
  { letter: 'ai', sound: 'āi', example: '白 (bái)', flipped: false },
  { letter: 'ei', sound: 'ēi', example: '黑 (hēi)', flipped: false },
  { letter: 'ui', sound: 'uī', example: '对 (duì)', flipped: false },
  { letter: 'ao', sound: 'āo', example: '好 (hǎo)', flipped: false },
  { letter: 'ou', sound: 'ōu', example: '手 (shǒu)', flipped: false },
  { letter: 'iu', sound: 'iū', example: '六 (liù)', flipped: false },
  { letter: 'ie', sound: 'iē', example: '叶 (yè)', flipped: false },
  { letter: 'üe', sound: 'üē', example: '月 (yuè)', flipped: false },
  { letter: 'er', sound: 'ěr', example: '耳 (ěr)', flipped: false },
  { letter: 'an', sound: 'ān', example: '山 (shān)', flipped: false },
  { letter: 'en', sound: 'ēn', example: '人 (rén)', flipped: false },
  { letter: 'in', sound: 'īn', example: '心 (xīn)', flipped: false },
  { letter: 'un', sound: 'ūn', example: '春 (chūn)', flipped: false },
  { letter: 'ün', sound: 'ǖn', example: '云 (yún)', flipped: false },
  { letter: 'ang', sound: 'āng', example: '长 (cháng)', flipped: false },
  { letter: 'eng', sound: 'ēng', example: '风 (fēng)', flipped: false },
  { letter: 'ing', sound: 'īng', example: '星 (xīng)', flipped: false },
  { letter: 'ong', sound: 'ōng', example: '中 (zhōng)', flipped: false }
])

// === 语文 ===
const poems = ref([])
const chineseClassroomId = ref(null)

async function loadPoems() {
  loading.value = true
  try {
    const res = await classroomApi.getAll()
    const chineseRoom = res.data.find(c => c.type === 'CHINESE')
    if (chineseRoom) {
      chineseClassroomId.value = chineseRoom.id
      try {
        const config = JSON.parse(chineseRoom.config || '{}')
        poems.value = config.poems || []
      } catch (e) {
        poems.value = []
      }
    }
  } catch (e) {
    console.error('加载诗词失败', e)
  } finally {
    loading.value = false
  }
}

// 学会标记 - 使用 localStorage 持久化（改名为"已学习"）
const LEARNED_KEY = 'classroom_learned_poems'
const learnedPoems = ref(new Set(JSON.parse(localStorage.getItem(LEARNED_KEY) || '[]')))

function isLearned(poem) {
  return learnedPoems.value.has(poem.title + '|' + poem.author)
}
function toggleLearned(poem) {
  const key = poem.title + '|' + poem.author
  if (learnedPoems.value.has(key)) {
    learnedPoems.value.delete(key)
  } else {
    learnedPoems.value.add(key)
  }
  learnedPoems.value = new Set(learnedPoems.value)
  localStorage.setItem(LEARNED_KEY, JSON.stringify([...learnedPoems.value]))
}

const learnedCount = computed(() => {
  return poems.value.filter(p => isLearned(p)).length
})

const filteredPoems = computed(() => {
  if (poemFilter.value === 'learned') return poems.value.filter(p => isLearned(p))
  if (poemFilter.value === 'unlearned') return poems.value.filter(p => !isLearned(p))
  return poems.value
})

const selectedPoem = ref(null)
const showPinyin = ref(false)
const dictationMode = ref(false)

function getPinyin(text) {
  if (!text) return ''
  return toPinyin(text)
}
const userInput = ref('')
const dictationResult = ref('')

function showPoem(poem) { selectedPoem.value = poem; showPinyin.value = false; dictationMode.value = false; userInput.value = ''; dictationResult.value = '' }
function speak(text) { const u = new SpeechSynthesisUtterance(text); u.lang = 'zh-CN'; u.rate = 0.8; speechSynthesis.speak(u) }
function speakPoem(poem) {
  let text = poem.title
  if (poem.author) text += '，' + poem.author
  if (poem.dynasty) text += '，' + poem.dynasty
  text += '。' + poem.content
  speak(text)
}
function startDictation() { dictationMode.value = true; dictationResult.value = '' }
function checkDictation() {
  const target = selectedPoem.value.content.replace(/\n/g, '').replace(/[，。？！、；：""''（）《》\s]/g, '')
  const input = userInput.value.replace(/[，。？！、；：""''（）《》\s]/g, '')
  dictationResult.value = target === input ? '正确！🎉' : '再想想～'
}

// 添加诗词
const showAddPoemModal = ref(false)
const newPoem = ref({ title: '', author: '', dynasty: '', content: '' })
const addError = ref('')

async function addPoem() {
  if (!newPoem.value.title || !newPoem.value.author || !newPoem.value.content) {
    addError.value = '请填写完整信息'
    return
  }
  if (!chineseClassroomId.value) {
    addError.value = '未找到语文教室'
    return
  }
  try {
    const poemData = {
      title: newPoem.value.title.trim(),
      author: newPoem.value.author.trim(),
      dynasty: newPoem.value.dynasty.trim() || '未知',
      content: newPoem.value.content.trim()
    }
    const res = await classroomApi.addPoem(chineseClassroomId.value, poemData)
    // 更新本地诗词列表
    const config = JSON.parse(res.data.config || '{}')
    poems.value = config.poems || []
    showAddPoemModal.value = false
    newPoem.value = { title: '', author: '', dynasty: '', content: '' }
    addError.value = ''
  } catch (e) {
    addError.value = e.response?.data?.error || '添加失败'
  }
}

async function deletePoem(poem) {
  if (!confirm(`确定删除《${poem.title}》吗？`)) return
  if (!chineseClassroomId.value) return
  try {
    const res = await classroomApi.removePoem(chineseClassroomId.value, poem.title)
    const config = JSON.parse(res.data.config || '{}')
    poems.value = config.poems || []
    selectedPoem.value = null
  } catch (e) {
    alert(e.response?.data?.error || '删除失败')
  }
}

// 声调数据（4声）
const toneList = ref([
  { tone: 1, icon: '🔵', mark: 'ā', name: '高平', desc: '声音高而平稳，不升不降', example: '妈 mā / 诗 shī / 天 tiān / 哥 gē' },
  { tone: 2, icon: '🟡', mark: 'á', name: '高升', desc: '声音从中高升到高', example: '麻 má / 骑 qí / 来 lái / 拔 bá' },
  { tone: 3, icon: '🟠', mark: 'ǎ', name: '降升', desc: '先降再升，曲调先低后高', example: '马 mǎ / 我 wǒ / 走 zǒu / 有 yǒu' },
  { tone: 4, icon: '🔴', mark: 'à', name: '高降', desc: '声音从高快速降到低', example: '骂 mà / 气 qì / 去 qù / 爸 bà' }
])

// 声调发声
function speakTone(t) {
  speak(t.mark + '（第' + t.tone + '声，' + t.name + '）')
  setTimeout(() => speak(t.example), 800)
}

// 声母/韵母发声
const pinyinCharMap = {
  // 声母
  'bō':'波','pō':'坡','mō':'摸','fō':'佛',
  'dē':'得','tē':'特','nē':'讷','lē':'乐',
  'gē':'哥','kē':'科','hē':'喝',
  'jī':'机','qī':'七','xī':'西',
  'zhī':'知','chī':'吃','shī':'师','rì':'日',
  'zī':'资','cī':'雌','sī':'思','yī':'衣','wū':'屋',
  // 韵母
  'ā':'啊','ō':'哦','ē':'婀','ī':'衣','ū':'乌',
  'ǖ':'淤',
  'āi':'哀','ēi':'诶','uī':'威','āo':'凹','ōu':'欧','iū':'优',
  'iē':'椰','üē':'约','ěr':'耳',
  'ān':'安','ēn':'恩','īn':'音','ūn':'温','ǖn':'晕',
  'āng':'昂','ēng':'鞥','īng':'英','ōng':'中'
}
function speakPinyin(item) {
  const text = pinyinCharMap[item.sound] || item.sound
  speak(text)
}

function speakLetter(text) {
  const u = new SpeechSynthesisUtterance(text)
  u.lang = 'en-US'
  u.rate = 0.85
  speechSynthesis.speak(u)
}
function speakAllLetters() { letters.forEach((l, i) => setTimeout(() => speakLetter(l.lower), i * 600)) }

// === 数学 ===
const mathMode = ref('arithmetic')
const mathConfig = reactive({ maxNum: 10, operations: ['+', '-'], count: 10 })
const mathProblems = ref([])

// 英语测验
const englishMode = ref('cards')
const englishQuizActive = ref(false)
const englishQuizQuestions = ref([])
const englishQuizIndex = ref(0)
const englishQuizScore = ref(0)
const englishQuizSelected = ref(null)
const englishQuizSubmitted = ref(false)

// 诗词填空测验
const poemQuizActive = ref(false)
const poemQuizBlanks = ref([])
const poemQuizIndex = ref(0)
const poemQuizScore = ref(0)
const poemQuizAnswers = ref([])
const poemQuizChecked = ref(false)

// 数学交卷
const mathSubmitted = ref(false)
const mathSubmittedScore = ref(0)
const multiplySubmitted = ref(false)
const multiplySubmittedScore = ref(0)
const isStudent = ref(false)
const wrongRecords = ref([])

function _makeOneProblem() {
  if (!mathConfig.operations.length) return null
  const op = mathConfig.operations[Math.floor(Math.random() * mathConfig.operations.length)]
  let a = Math.floor(Math.random() * mathConfig.maxNum) + 1
  let b = Math.floor(Math.random() * mathConfig.maxNum) + 1
  if (op === '÷') { a = a * b }
  if (op === '-' && a < b) { [a, b] = [b, a] }
  const ans = { '+': a+b, '-': a-b, '×': a*b, '÷': a/b }
  return { question: a + ' ' + op + ' ' + b + ' = ?', answer: ans[op], userAnswer: null, result: null, showAnswer: false }
}

function generateMathProblems() {
  if (!mathConfig.operations.length) return alert('请选择运算')
  const cnt = Math.max(1, Math.min(50, mathConfig.count))
  mathProblems.value = Array.from({ length: cnt }, () => _makeOneProblem())
}

function checkOneProblem(i) {
  const p = mathProblems.value[i]
  if (p.result !== null) return
  if (Number(p.userAnswer) === p.answer) {
    p.result = true
  } else {
    p.result = false
    wrongRecords.value.push(p.question + ' 你答：' + p.userAnswer)
  }
}

function peekAnswer(i) {
  mathProblems.value[i].showAnswer = !mathProblems.value[i].showAnswer
}

// 九九乘法表
const multiplyMode = ref('table')
const multiplyConfig = reactive({ count: 10 })
const multiplyProblems = ref([])

function generateMultiplyQuiz() {
  const cnt = Math.max(1, Math.min(50, multiplyConfig.count))
  multiplyProblems.value = Array.from({ length: cnt }, () => {
    const a = Math.floor(Math.random() * 9) + 1
    const b = Math.floor(Math.random() * 9) + 1
    return { question: a + ' × ' + b + ' = ?', answer: a * b, userAnswer: null, result: null, showAnswer: false }
  })
}

function checkMultiplyOne(i) {
  const p = multiplyProblems.value[i]
  if (p.result !== null) return
  if (Number(p.userAnswer) === p.answer) {
    p.result = true
  } else {
    p.result = false
    wrongRecords.value.push(p.question + ' 你答：' + p.userAnswer)
  }
}

// === 英语 ===
const letters = ref('ABCDEFGHIJKLMNOPQRSTUVWXYZ'.split('').map(l => ({ upper: l, lower: l.toLowerCase(), flipped: false })))

onMounted(() => {
  loadPoems()
  generateMathProblems()
  const authStore = useAuthStore()
  isStudent.value = authStore.isStudent
})

// === 功能函数 ===
function startEnglishQuiz() {
  englishMode.value = 'quiz'
  englishQuizActive.value = true
  englishQuizIndex.value = 0
  englishQuizScore.value = 0
  englishQuizSelected.value = null
  englishQuizSubmitted.value = false
  const shuffled = [...letters.value].sort(() => Math.random() - 0.5).slice(0, 10)
  englishQuizQuestions.value = shuffled.map(letter => {
    const opts = [letter.upper, ...'ABCDEFGHIJKLMNOPQRSTUVWXYZ'.split('').filter(u => u !== letter.upper).sort(() => Math.random() - 0.5).slice(0, 3)]
    return { letter, options: opts.sort(() => Math.random() - 0.5) }
  })
}

function submitEnglishAnswer() {
  const q = englishQuizQuestions.value[englishQuizIndex.value]
  if (englishQuizSelected.value === q.letter.upper) englishQuizScore.value++
  englishQuizSubmitted.value = true
}

function nextEnglishQuestion() {
  englishQuizIndex.value++
  englishQuizSelected.value = null
  englishQuizSubmitted.value = false
  if (englishQuizIndex.value >= englishQuizQuestions.value.length) {
    studentApi2.quizScore(englishQuizScore.value, '英语字母测验').catch(() => {})
  }
}

function startPoemQuiz() {
  poemQuizActive.value = true
  poemQuizIndex.value = 0
  poemQuizScore.value = 0
  poemQuizChecked.value = false
  poemQuizAnswers.value = []
  const shuffled = [...poems.value].sort(() => Math.random() - 0.5).slice(0, 5)
  poemQuizBlanks.value = shuffled.map(poem => {
    const chars = poem.content.split('').filter(c => c !== '\n')
    const blanks = []
    const usedIdx = new Set()
    for (let i = 0; i < Math.min(4, chars.length); i++) {
      let idx
      do { idx = Math.floor(Math.random() * chars.length) } while (usedIdx.has(idx) || /[，。！？、；：""''（）\s]/.test(chars[idx]))
      usedIdx.add(idx)
      blanks.push({ char: chars[idx], pos: idx })
    }
    // 关键：blanks 按位置升序排列，确保输入框顺序与下划线位置一致
    blanks.sort((a, b) => a.pos - b.pos)
    const blankPositions = blanks.map(b => b.pos)
    return { poem, blanks, blankPositions }
  })
}

function checkPoemBlanks() {
  poemQuizChecked.value = true
  const blanks = poemQuizBlanks.value[poemQuizIndex.value].blanks
  blanks.forEach((blank, bi) => {
    if (poemQuizAnswers.value[bi] === blank.char) poemQuizScore.value++
  })
}

function nextPoemQuestion() {
  poemQuizIndex.value++
  poemQuizChecked.value = false
  poemQuizAnswers.value = []
  if (poemQuizIndex.value >= poemQuizBlanks.value.length) {
    studentApi2.quizScore(poemQuizScore.value, '语文诗词填空测验').catch(() => {})
  }
}

function submitMathScore() {
  const correct = mathProblems.value.filter(p => p.result).length
  mathSubmitted.value = true
  mathSubmittedScore.value = correct
  studentApi2.quizScore(correct, '数学四则运算').catch(() => {})
}

function submitMultiplyScore() {
  const correct = multiplyProblems.value.filter(p => p.result).length
  multiplySubmitted.value = true
  multiplySubmittedScore.value = correct
  studentApi2.quizScore(correct, '数学乘法练习').catch(() => {})
}
</script>
<style scoped>
.classroom-container { padding: 20px; max-width: 800px; margin: 0 auto; min-height: 100vh; background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%); }
.classroom-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.classroom-header h1 { color: #92400e; font-size: 2rem; }
.back-btn { padding: 8px 16px; background: rgba(255,255,255,0.6); color: #92400e; border: none; border-radius: 20px; cursor: pointer; font-size: 1rem; }
.back-btn:hover { background: rgba(255,255,255,0.9); }
.tabs { display: flex; gap: 10px; margin-bottom: 20px; }
.tab-btn { flex: 1; padding: 12px; background: rgba(255,255,255,0.5); border: none; border-radius: 20px; cursor: pointer; font-size: 1rem; transition: all 0.3s; }
.tab-btn.active { background: #d97706; color: white; }
.tab-content { animation: fadeIn 0.3s; }
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
.loading, .empty { text-align: center; padding: 40px; color: #92400e; font-size: 1.1rem; }

/* 语文 - 工具栏 */
.poem-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px; padding: 10px 15px; background: rgba(255,255,255,0.6); border-radius: 12px; flex-wrap: wrap; gap: 8px; }
.poem-stats { display: flex; gap: 15px; color: #92400e; font-size: 0.95rem; }
.learned-count { color: #059669; font-weight: 600; }
.poem-actions { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.add-poem-btn { padding: 6px 14px; background: #059669; color: white; border: none; border-radius: 15px; cursor: pointer; font-size: 0.9rem; }
.add-poem-btn:hover { background: #047857; }
.poem-filter { display: flex; gap: 6px; }
.filter-btn { padding: 5px 14px; border: 1px solid #d97706; background: white; color: #92400e; border-radius: 15px; cursor: pointer; font-size: 0.85rem; transition: all 0.2s; }
.filter-btn.active { background: #d97706; color: white; }
.filter-btn:hover { opacity: 0.85; }

/* 语文 - 诗词列表 */
.poem-list { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 12px; }
.poem-card { padding: 14px; background: white; border-radius: 15px; cursor: pointer; transition: all 0.2s; box-shadow: 0 2px 10px rgba(0,0,0,0.05); border-left: 4px solid transparent; }
.poem-card:hover { transform: translateY(-3px); box-shadow: 0 6px 20px rgba(0,0,0,0.1); }
.poem-card.learned { border-left-color: #059669; background: linear-gradient(135deg, #f0fdf4, white); }
.poem-card-header { display: flex; justify-content: space-between; align-items: flex-start; }
.poem-card h3 { margin: 0; color: #92400e; font-size: 1rem; }
.poem-card .author { margin: 4px 0 0; color: #666; font-size: 0.85rem; }
.poem-card .dynasty { color: #999; font-size: 0.8rem; }
.poem-card .preview { margin: 6px 0 0; color: #999; font-size: 0.8rem; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.learn-btn { background: none; border: none; cursor: pointer; font-size: 1.1rem; padding: 0 2px; line-height: 1; transition: transform 0.2s; }
.learn-btn:hover { transform: scale(1.3); }
.learn-btn.is-learned { color: #059669; }

/* 弹窗 */
.modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal { background: white; padding: 25px; border-radius: 15px; max-width: 500px; width: 90%; max-height: 80vh; overflow-y: auto; }
.modal-header { display: flex; justify-content: space-between; align-items: center; }
.modal-header h2 { margin: 0; color: #92400e; }
.modal-header-actions { display: flex; align-items: center; gap: 10px; }
.learn-btn-lg { padding: 6px 14px; border: 2px solid #d97706; background: white; color: #92400e; border-radius: 20px; cursor: pointer; font-size: 0.9rem; transition: all 0.2s; }
.learn-btn-lg.is-learned { background: #059669; color: white; border-color: #059669; }
.modal-actions .active { background: #0369a1; }
.close-btn { background: none; border: none; font-size: 24px; cursor: pointer; color: #999; }
.modal-author { color: #666; font-style: italic; margin: 10px 0; }
.modal-author .dynasty { color: #999; }
.modal-content { background: #fef3c7; padding: 20px; border-radius: 10px; white-space: pre-wrap; font-size: 18px; line-height: 2; font-family: 'KaiTi', serif; }
.modal-pinyin { background: #e0f2fe; padding: 15px 20px; border-radius: 10px; white-space: pre-wrap; font-size: 16px; line-height: 2; color: #0369a1; margin-top: 8px; border: 2px dashed #7dd3fc; font-family: 'KaiTi', serif; }
.modal-actions { display: flex; gap: 10px; margin-top: 15px; flex-wrap: wrap; }
.modal-actions button { padding: 10px 20px; background: #d97706; color: white; border: none; border-radius: 8px; cursor: pointer; font-size: 1rem; }
.modal-actions .delete-btn { background: #dc2626; }
.dictation-area { margin-top: 20px; padding-top: 15px; border-top: 1px solid #eee; }
.dictation-area textarea { width: 100%; height: 100px; margin: 10px 0; padding: 10px; border: 2px solid #fde68a; border-radius: 8px; font-size: 16px; resize: none; }
.dictation-area button { padding: 8px 20px; background: #059669; color: white; border: none; border-radius: 8px; cursor: pointer; }
.correct { color: #059669; font-weight: bold; font-size: 1.2rem; }
.wrong { color: #dc2626; font-weight: bold; font-size: 1.2rem; }

/* 添加诗词弹窗 */
.add-poem-modal { max-width: 450px; }
.add-poem-form { display: flex; flex-direction: column; gap: 12px; margin-top: 15px; }
.add-poem-form label { display: flex; flex-direction: column; gap: 4px; color: #666; font-size: 0.9rem; }
.add-poem-form input, .add-poem-form textarea { padding: 10px; border: 2px solid #fde68a; border-radius: 8px; font-size: 1rem; }
.add-poem-form textarea { height: 120px; resize: none; }
.add-poem-form .submit-btn { padding: 12px; background: #059669; color: white; border: none; border-radius: 8px; cursor: pointer; font-size: 1rem; margin-top: 8px; }
.add-poem-form .submit-btn:disabled { background: #ccc; cursor: not-allowed; }
.add-poem-form .error { color: #dc2626; font-size: 0.9rem; text-align: center; }

/* 数学 */
.math-section-toggle { display: flex; gap: 10px; margin-bottom: 15px; }
.toggle-btn { flex: 1; padding: 10px; background: rgba(255,255,255,0.5); border: 2px solid transparent; border-radius: 15px; cursor: pointer; font-size: 1rem; transition: all 0.3s; color: #92400e; }
.toggle-btn.active { background: #d97706; color: white; border-color: #b45309; }
.math-config { background: white; padding: 20px; border-radius: 15px; margin-bottom: 20px; box-shadow: 0 2px 10px rgba(0,0,0,0.05); }
.config-row { display: flex; gap: 15px; flex-wrap: wrap; margin-bottom: 10px; }
.math-config label { margin-right: 15px; color: #666; }
.math-config input[type="number"] { width: 80px; padding: 6px; border: 2px solid #fde68a; border-radius: 8px; font-size: 1rem; }
.checkbox-group { margin: 10px 0; display: flex; gap: 15px; flex-wrap: wrap; }
.checkbox-group label { display: flex; align-items: center; gap: 4px; cursor: pointer; }
.gen-btn { padding: 10px 25px; background: #d97706; color: white; border: none; border-radius: 8px; cursor: pointer; font-size: 1rem; margin-top: 10px; }
.gen-btn:hover { background: #b45309; }

/* 题目列表 */
.math-quiz-list { display: flex; flex-direction: column; gap: 12px; }
.math-quiz-item { padding: 15px; background: white; border-radius: 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.05); transition: all 0.2s; }
.math-quiz-item.answered { opacity: 0.85; }
.quiz-question { display: flex; align-items: center; gap: 10px; font-size: 1.2rem; color: #92400e; }
.quiz-index { font-weight: bold; min-width: 28px; color: #d97706; }
.quiz-text { flex: 1; }
.peek-btn { background: none; border: none; cursor: pointer; font-size: 1.3rem; padding: 2px 6px; border-radius: 6px; transition: background 0.2s; }
.peek-btn:hover { background: #fde68a; }
.peek-answer { padding: 8px 0; color: #059669; font-size: 1rem; font-weight: 600; }
.quiz-input { display: flex; align-items: center; gap: 10px; margin-top: 8px; }
.quiz-input input { width: 100px; padding: 8px; font-size: 1.1rem; text-align: center; border: 2px solid #fde68a; border-radius: 8px; }
.quiz-input button { padding: 8px 18px; background: #d97706; color: white; border: none; border-radius: 8px; cursor: pointer; }
.quiz-summary { text-align: center; padding: 20px; background: white; border-radius: 12px; margin-top: 10px; }
.quiz-summary p { font-size: 1.2rem; color: #059669; font-weight: bold; margin-bottom: 10px; }

/* 九九乘法表 */
.multiply-section { animation: fadeIn 0.3s; }
.multiply-toolbar { display: flex; gap: 10px; margin-bottom: 15px; }
.multiply-quiz-config { background: white; padding: 15px 20px; border-radius: 12px; margin-bottom: 15px; display: flex; align-items: center; gap: 15px; box-shadow: 0 2px 8px rgba(0,0,0,0.05); }
.multiply-quiz-config label { color: #666; }
.multiply-quiz-config input { width: 80px; padding: 6px; border: 2px solid #fde68a; border-radius: 8px; font-size: 1rem; }
.multiply-table-wrap { overflow-x: auto; background: white; border-radius: 15px; padding: 15px; box-shadow: 0 2px 10px rgba(0,0,0,0.05); }
.multiply-table { border-collapse: collapse; width: 100%; font-size: 0.95rem; text-align: center; }
.multiply-table th { background: #d97706; color: white; padding: 8px 6px; font-weight: 600; }
.multiply-table td { padding: 8px 6px; border: 1px solid #fde68a; color: #92400e; transition: background 0.2s; }
.multiply-table td.highlight { background: #fef3c7; font-weight: 600; }
.multiply-table td:hover { background: #fde68a; }
.multiply-table th:first-child { border-radius: 8px 0 0 0; }
.multiply-table th:last-child { border-radius: 0 8px 0 0; }

/* 隐藏旧单题模式 */
.math-problem { text-align: center; padding: 40px 20px; background: white; border-radius: 15px; box-shadow: 0 4px 15px rgba(0,0,0,0.08); }
.wrong-records { margin-top: 25px; padding: 15px; background: #fff5f5; border-radius: 12px; }
.wrong-records h3 { color: #dc2626; margin-top: 0; }
.wrong-records ul { list-style: none; padding: 0; }
.wrong-records li { padding: 8px 0; border-bottom: 1px solid #fecaca; color: #666; }
.wrong-records button { padding: 6px 15px; background: #fecaca; color: #dc2626; border: none; border-radius: 6px; cursor: pointer; margin-top: 10px; }

/* 英语 */
.english-cards { display: grid; grid-template-columns: repeat(auto-fill, minmax(70px, 1fr)); gap: 10px; }
.letter-card { height: 80px; perspective: 1000px; cursor: pointer; position: relative; }
.letter-card > div { position: absolute; width: 100%; height: 100%; backface-visibility: hidden; display: flex; align-items: center; justify-content: center; border-radius: 10px; font-size: 28px; font-weight: bold; transition: transform 0.5s; }
.card-front { background: #d97706; color: white; }
.card-back { background: #059669; color: white; transform: rotateY(180deg); }
.letter-card.flipped .card-front { transform: rotateY(180deg); }
.letter-card.flipped .card-back { transform: rotateY(0deg); }
.hint { text-align: center; color: #92400e; margin-top: 20px; font-size: 0.9rem; }

/* 语文子模式切换 */
.chinese-sub-tabs { display: flex; gap: 10px; margin-bottom: 15px; }
.sub-tab-btn { flex: 1; padding: 10px; background: rgba(255,255,255,0.5); border: 2px solid transparent; border-radius: 15px; cursor: pointer; font-size: 1rem; transition: all 0.3s; color: #92400e; }
.sub-tab-btn.active { background: #059669; color: white; border-color: #047857; }

/* 英语卡片 */
.english-toolbar { display: flex; gap: 10px; margin-bottom: 15px; flex-wrap: wrap; }
.speak-all-btn { padding: 8px 16px; background: #3b82f6; color: white; border: none; border-radius: 20px; cursor: pointer; font-size: 0.85rem; transition: background 0.2s; }
.speak-all-btn:hover { background: #2563eb; }
.card-speak-btn { position: absolute; top: 4px; right: 4px; background: rgba(255,255,255,0.9); border: none; border-radius: 50%; width: 22px; height: 22px; font-size: 12px; cursor: pointer; display: flex; align-items: center; justify-content: center; }
.card-speak-btn:hover { background: white; }
.card-front, .card-back { position: relative; }

/* 拼音学习 */
.pinyin-card .speak-btn { position: absolute; top: 4px; right: 4px; background: rgba(0,0,0,0.25); border: none; cursor: pointer; font-size: 14px; border-radius: 50%; width: 24px; height: 24px; display: flex; align-items: center; justify-content: center; }
.pinyin-card .speak-btn:hover { background: rgba(0,0,0,0.4); }

/* 声调学习 */
.tone-section { animation: fadeIn 0.3s; }
.tone-intro { text-align: center; color: #666; font-size: 0.95rem; margin-bottom: 15px; padding: 10px; background: rgba(255,255,255,0.6); border-radius: 10px; }
.tone-cards { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 15px; }
.tone-card { background: white; border-radius: 16px; padding: 20px 15px; text-align: center; box-shadow: 0 4px 15px rgba(0,0,0,0.08); transition: transform 0.2s; }
.tone-card:hover { transform: translateY(-4px); }
.tone-icon { font-size: 2.5rem; margin-bottom: 8px; }
.tone-number { font-size: 1rem; font-weight: bold; color: #666; margin-bottom: 4px; }
.tone-mark { font-size: 2rem; font-weight: bold; color: #d97706; margin-bottom: 6px; font-family: 'KaiTi', serif; }
.tone-name { font-size: 1rem; color: #92400e; font-weight: 600; margin-bottom: 4px; }
.tone-desc { font-size: 0.8rem; color: #888; margin-bottom: 8px; line-height: 1.4; }
.tone-example { font-size: 0.85rem; color: #666; margin-bottom: 12px; line-height: 1.6; }
.tone-speak-btn { padding: 8px 20px; background: #d97706; color: white; border: none; border-radius: 20px; cursor: pointer; font-size: 0.9rem; transition: background 0.2s; }
.tone-speak-btn:hover { background: #b45309; }
.tone-1 { border-top: 4px solid #3b82f6; }
.tone-2 { border-top: 4px solid #eab308; }
.tone-3 { border-top: 4px solid #f97316; }
.tone-4 { border-top: 4px solid #ef4444; }

/* 英语卡片 */
.english-toolbar { display: flex; gap: 10px; margin-bottom: 15px; flex-wrap: wrap; }
.speak-all-btn { padding: 8px 16px; background: #3b82f6; color: white; border: none; border-radius: 20px; cursor: pointer; font-size: 0.85rem; transition: background 0.2s; }
.speak-all-btn:hover { background: #2563eb; }
.card-speak-btn { position: absolute; top: 4px; right: 4px; background: rgba(255,255,255,0.9); border: none; border-radius: 50%; width: 22px; height: 22px; font-size: 12px; cursor: pointer; display: flex; align-items: center; justify-content: center; }
.card-speak-btn:hover { background: white; }
.card-front, .card-back { position: relative; }

/* 拼音学习 */
.pinyin-section { animation: fadeIn 0.3s; }
.pinyin-toggle { display: flex; gap: 10px; margin-bottom: 15px; }
.pinyin-cards { display: grid; grid-template-columns: repeat(auto-fill, minmax(80px, 1fr)); gap: 10px; }
.pinyin-card { height: 90px; perspective: 1000px; cursor: pointer; position: relative; }
.pinyin-card > div { position: absolute; width: 100%; height: 100%; backface-visibility: hidden; display: flex; align-items: center; justify-content: center; border-radius: 12px; font-size: 24px; font-weight: bold; transition: transform 0.5s; flex-direction: column; }
.pinyin-front { background: linear-gradient(135deg, #d97706, #b45309); color: white; font-size: 28px; }
.pinyin-back { background: linear-gradient(135deg, #059669, #047857); color: white; transform: rotateY(180deg); padding: 8px; font-size: 14px; }
.pinyin-card.flipped .pinyin-front { transform: rotateY(180deg); }
.pinyin-card.flipped .pinyin-back { transform: rotateY(0deg); }
.pinyin-sound { font-size: 20px; font-weight: bold; margin-bottom: 4px; }
.pinyin-example { font-size: 12px; opacity: 0.9; }

/* 诗词填空测验 */
.poem-quiz-section { padding: 15px; animation: fadeIn 0.3s; }
.poem-quiz-card { background: white; border-radius: 15px; padding: 20px; text-align: center; box-shadow: 0 4px 15px rgba(0,0,0,0.08); }
.poem-quiz-title { color: #92400e; font-size: 1.1rem; font-weight: bold; margin-bottom: 15px; }
.poem-quiz-content { background: #fef3c7; padding: 20px; border-radius: 10px; font-size: 1.4rem; line-height: 2.5; font-family: 'KaiTi', serif; white-space: pre-wrap; margin-bottom: 20px; }
.poem-blank { display: inline-block; border-bottom: 2px solid #d97706; color: #d97706; min-width: 1.8em; text-align: center; }
.poem-blank.revealed { border-bottom-color: #059669; color: #059669; }
.poem-blanks-hint { color: #666; font-size: 0.9rem; margin-bottom: 10px; }
.poem-blanks-row { display: flex; gap: 10px; justify-content: center; flex-wrap: wrap; margin-bottom: 15px; }
.poem-blank-input { width: 60px; height: 50px; text-align: center; font-size: 1.4rem; border: 2px solid #d97706; border-radius: 8px; font-family: 'KaiTi', serif; }
.poem-blank-input:focus { outline: none; border-color: #b45309; background: #fffbeb; }

/* 英语测验 */
.english-quiz-section { padding: 15px; text-align: center; }
.english-quiz-card { background: white; border-radius: 15px; padding: 20px; text-align: center; box-shadow: 0 4px 15px rgba(0,0,0,0.08); margin-top: 10px; }
.quiz-choices { display: flex; gap: 12px; justify-content: center; flex-wrap: wrap; margin: 16px 0; }
.quiz-choice-btn { padding: 12px 20px; background: white; border: 2px solid #d97706; border-radius: 10px; cursor: pointer; font-size: 1.1rem; min-width: 60px; transition: all 0.2s; }
.quiz-choice-btn:hover { background: #fef3c7; }
.quiz-choice-btn.selected { background: #d97706; color: white; }
.quiz-choice-btn.correct { background: #059669; color: white; border-color: #059669; }
.quiz-choice-btn.wrong { background: #dc2626; color: white; border-color: #dc2626; }
.quiz-choice-btn:disabled { cursor: default; }
.submit-quiz-btn { margin-top: 16px; padding: 12px 32px; background: #059669; color: white; border: none; border-radius: 8px; cursor: pointer; font-size: 1rem; }
.submit-quiz-btn:hover { background: #047857; }
.submit-quiz-btn:disabled { background: #ccc; cursor: not-allowed; }
.next-quiz-btn { margin-top: 10px; margin-left: 8px; padding: 10px 24px; background: #d97706; color: white; border: none; border-radius: 8px; cursor: pointer; font-size: 1rem; }
.next-quiz-btn:hover { background: #b45309; }
.quiz-feedback { margin-top: 10px; padding: 10px; background: #fef3c7; border-radius: 8px; font-size: 1rem; }
.quiz-finish { background: white; border-radius: 15px; padding: 30px; text-align: center; box-shadow: 0 4px 15px rgba(0,0,0,0.08); margin-top: 10px; }
.finish-score { font-size: 1.3rem; color: #059669; font-weight: bold; margin-bottom: 20px; }
.gen-btn.secondary { background: #666; }
.gen-btn.secondary:hover { background: #444; }
.quiz-header-bar { display: flex; justify-content: space-between; align-items: center; padding: 10px 15px; background: rgba(255,255,255,0.6); border-radius: 12px; margin-bottom: 10px; }
.quiz-progress { color: #92400e; font-weight: bold; }
.quiz-score-display { color: #059669; font-weight: bold; }
.quiz-target-letter { display: flex; align-items: center; justify-content: center; gap: 16px; margin-bottom: 12px; }
.target-char { font-size: 3rem; font-weight: bold; color: #92400e; }
.quiz-target-letter .speak-btn { padding: 8px 12px; background: #3b82f6; color: white; border: none; border-radius: 8px; cursor: pointer; font-size: 0.9rem; }
.quiz-target-letter .speak-btn:hover { background: #2563eb; }
.quiz-prompt { color: #666; margin-bottom: 12px; }
.english-quiz-placeholder { text-align: center; padding: 40px; color: #92400e; }
</style>