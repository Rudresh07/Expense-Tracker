# Android Agent — Universal CLAUDE.md
# Works on any Android project. Drop this in the project root.
# Claude learns your specific project on first run and saves context.

---

## How to use

Paste a ticket and say: "Run the agentic flow for this ticket"
Claude will follow the workflow below — stopping at every gate for your approval.

---

## Step 0 — Project learning (runs automatically on first session)

Before doing ANYTHING else, check if `.claude/project_context.md` exists.

If it EXISTS → read it and use it as the source of truth. Skip scanning.

If it does NOT exist → scan the project now:

### What to scan
Read these to understand the project:

1. `build.gradle` or `build.gradle.kts` → dependencies, SDK versions, libraries used
2. `app/src/main/AndroidManifest.xml` → package name, activities, permissions
3. Top 3 levels of `app/src/main/java/` or `app/src/main/kotlin/` → package structure
4. Pick 2-3 existing feature folders and read:
    - The ViewModel
    - The Repository (or UseCase if clean arch)
    - The Fragment or Activity
    - The API interface (Retrofit)
5. Any existing BaseViewModel, BaseFragment, BaseActivity, BaseRepository

### What to extract and save
After scanning, create `.claude/project_context.md` with:

```
# Project context — learned on [date]

## Package name
[com.example.app]

## Architecture
[MVVM / MVVM + Clean Architecture / MVI — be specific]
[Single module / Multi-module — list module names]

## Layer structure
[Describe exactly where each layer lives — actual package paths]

## Tech stack
- UI: [XML Views / Compose / Mixed]
- DI: [Hilt / Koin / Manual]
- Async: [Coroutines + Flow / LiveData / RxJava]
- Network: [Retrofit / Ktor / Other]
- DB: [Room / DataStore / None]
- Image: [Glide / Coil / Picasso]

## Base classes
- ViewModel extends: [ViewModel() / BaseViewModel — show constructor]
- Fragment extends: [Fragment / BaseFragment — show example]
- Repository pattern: [interface + impl / single class]

## Naming conventions
- ViewModel: [LoginViewModel, HomeViewModel]
- Repository: [LoginRepository + LoginRepositoryImpl / LoginRepo]
- Fragment: [LoginFragment]
- Layout files: [fragment_login.xml / login_fragment.xml]
- API interface: [LoginApiService / LoginApi]

## Data flow
[Step by step — e.g. Retrofit call → Repository → ViewModel → Fragment]

## API response handling
[Result<T> / Sealed class / Direct — show actual pattern used]

## Error handling pattern
[How errors flow from Repository to ViewModel to UI — show actual code pattern]

## Existing feature example
[Pick one feature and describe exactly how it's implemented end to end]

## Things this project always does
[Conventions that appear in every feature — list them]

## Things this project never does
[Anti-patterns avoided — list them]

## Min SDK / Target SDK
[minSdk X / targetSdk Y]
```

After saving — confirm to the developer: "Project learned. Context saved to .claude/project_context.md"

---

## Agentic workflow — follow this for EVERY ticket

### Agent 1 — Specification

Read the ticket fully. Then output:

**REQUIREMENTS**
[numbered list of every functional requirement]

**NON-FUNCTIONAL REQUIREMENTS**
[performance, offline, permissions, SDK, dark mode, rotation]

**AMBIGUITIES**
[anything unclear — do not guess, list it]

**AFFECTED AREAS**
[which existing screens, flows, classes are touched]

**DEPENDENCIES**
[other features, APIs, or classes this depends on]

**SUMMARY**
[2-3 sentences: what this does and why]

⛔ STOP HERE. Write:
"Agent 1 complete. Type APPROVE to continue to design, or give corrections."
Do not proceed until developer types APPROVE.

---

### Agent 2 — Design plan

Using the project context from `.claude/project_context.md` and the approved spec:

**FILES TO CREATE**
[full path | class type | responsibility — one per line]

**FILES TO MODIFY**
[full path | what changes and where]

**DATA FLOW**
[step by step for this specific feature using actual class names from the project]

**ANDROID CONSIDERATIONS**
[permissions needed, lifecycle edge cases, offline behavior, config change, min SDK]

**ACCEPTANCE CRITERIA**
[Given/When/Then — minimum 4:
- 1 happy path
- 1 error state
- 1 edge case
- 1 Android-specific (rotation, offline, permission denied)]

**BRANCH NAME**
feature/[TICKET-ID]-[short-description-kebab-case]

**DO NOT:**
[List what should explicitly NOT be done based on the ticket]

⛔ STOP HERE. Write:
"Agent 2 complete. Review the plan and ACs above. Type APPROVE to start writing code, or give corrections."
Do not write any code until developer types APPROVE.

---

### Agent 3 — Implementation

Only runs after APPROVE on Agent 2.

Rules:
- Follow `.claude/project_context.md` exactly — match existing patterns, naming, and structure
- If the project uses BaseViewModel → extend it
- If the project uses a specific error handling pattern → use it
- If the project uses ViewBinding → use ViewBinding
- Match the exact same style as existing files in the project
- Write one file at a time — announce each file before writing it
- After writing each file, say: "✅ [filename] written"
- Never hardcode strings — add comment: // strings.xml: <string name="KEY">value</string>
- Never use GlobalScope — use viewModelScope or lifecycleScope
- Never use findViewById — ViewBinding or Compose only
- Never introduce a library or pattern not already in the project

---

### Agent 4 — Self-review

After all files are written, review your own output:

**AC CHECK**
[For each AC: ✅ PASS or ❌ FAIL with reason]

**CONVENTION CHECK**
✅/❌ Matches existing architecture
✅/❌ Matches existing naming conventions
✅/❌ Matches existing DI pattern
✅/❌ Matches existing error handling
✅/❌ No hardcoded strings
✅/❌ No GlobalScope
✅/❌ No findViewById
✅/❌ No new libraries introduced
✅/❌ No business logic in Fragment/Activity

**VERDICT**
PASS — ready for PR
or
FAIL — [specific issues found, which files to fix]

If FAIL → fix the issues automatically, then re-run the AC check.

⛔ STOP HERE. Write:
"Agent 4 complete. Review the verification above. Type APPROVE to generate PR description."

---

### Agent 5 — PR description

Generate a ready-to-paste GitHub PR description:

```
## Summary
[2-3 sentences: what changed and why]

## Jira ticket
[TICKET-ID]

## Changes
[- filename → what it does]

## Acceptance criteria
- [ ] Given...
- [ ] Given...

## Testing notes
[what reviewer should manually test]

## Screenshots
_Add screenshots if UI changed_
```

Then write:
"✅ Pipeline complete. Branch: [branch name]. Copy the PR description above."

---

## Universal Android rules — always follow these

- ViewBinding only — never findViewById
- viewModelScope for coroutines in ViewModel
- lifecycleScope for coroutines in Fragment/Activity
- Never put API calls in ViewModel — Repository only
- Never put business logic in Fragment/Activity
- Never use GlobalScope
- Never introduce a new Gradle module
- All user-facing strings in strings.xml
- Match whatever DI framework is already in the project
- Match whatever async pattern is already in the project

---

## Memory management

After learning the project — update `.claude/project_context.md` whenever:
- You discover a new pattern not previously documented
- Developer corrects something you got wrong
- A new feature is added that introduces a new pattern

This keeps the context fresh without re-scanning every time.

---

## Ticket format (paste in this format for best results)

```
TICKET: [ID]
SUMMARY: [one line]
DESCRIPTION: [full description — entry point, user action, success, failure, exit]
API ENDPOINTS: [method, path, request, response, error codes]
FIGMA: [link if UI change]
AFFECTED SCREENS: [which screens/flows]
EDGE CASES: [list all non-happy-path scenarios]
DO NOT: [explicit constraints]
MIN SDK: [if specific to this ticket]
PERMISSIONS: [if needed]
```