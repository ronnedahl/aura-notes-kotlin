## Project: Aura Notes
Voice-first Android notes app. Native Kotlin + Jetpack Compose. The user records
a voice note, it is transcribed with Android's on-device SpeechRecognizer, and
stored locally in a Room database. Features: categories (Personal, Work, Ideas,
Shopping, None), search, favorites, share, .txt export, and a home-screen widget
for one-tap recording.
This is a personal portfolio and learning project. No backend, no network calls,
fully offline.
Owner and roles

## Peter (GitHub: ronnedahl) is the human in the loop.
Peter is the ONLY person who merges to main, and the ONLY physical tester. He
installs the debug APK on a Samsung Galaxy A33 (Exynos 1280, Android 13/14) and
tests on the real device.
Claude Code writes code, runs builds to verify it compiles, and prepares pull
requests. Claude Code NEVER merges and NEVER pushes to main directly.

## Tech stack and conventions

Kotlin only. UI is 100% Jetpack Compose. No XML layouts, no Java, no NDK.
Architecture is MVVM and must stay that way:
Room (Entity / Dao / Database / Repository) -> AndroidViewModel exposing StateFlow
-> Compose UI. Do not collapse layers or put data access in the UI.
Namespace and applicationId: dev.peterbot.auranotes
minSdk 24. Set targetSdk and compileSdk to the current stable Android API.
Pick AGP, Kotlin, and Compose BOM versions that are mutually compatible and
known-stable. Do NOT use preview or canary AGP (the original AI Studio export
used canary AGP and would not sync cleanly). If a version is uncertain, state
the assumption in the PR and choose the latest stable.
Speech: use Android's SpeechRecognizer with Locale.getDefault() so it
transcribes Swedish on Peter's device. Request RECORD_AUDIO as a runtime
permission before the first recording.
NO networking. Do not add Retrofit, OkHttp, Moshi, Firebase, or any Gemini /
cloud AI dependency. Everything runs on-device. Keep dependencies minimal and
remove anything unused.

## Suggested package layout (under dev.peterbot.auranotes)

data/local/ : NoteEntity, NoteDao, NoteDatabase
data/repository/ : NoteRepository
viewmodel/ : NoteViewModel (AndroidViewModel)
speech/ : SpeechManager (wraps SpeechRecognizer)
ui/ : NoteScreen and ui/theme/
widget/ : AuraNotesWidgetProvider

Build and verification

Before opening any PR, run ./gradlew assembleDebug and confirm it compiles
cleanly. Address reasonable warnings.
Claude Code cannot test the UI. After a successful build, the PR description
must list what Peter should manually test on the A33 (for example: "record a
Swedish note, confirm it saves and appears in the list, test the widget
one-tap recording, test .txt export").
CI is not set up yet. A GitHub Actions workflow could build the debug APK on
push and upload it as an artifact. Ask Peter before adding it.

Git workflow (the core automation contract, follow exactly)
main is protected. Only Peter merges. Never commit or push directly to main.
For each task:

Start clean and current:

git checkout main
git pull origin main


Create a task branch (one branch per task):

git checkout -b feat/<short-task-name> (prefixes: feat/, fix/, chore/)


Work in small, focused commits with conventional messages:

feat: add category filter chips
fix: request RECORD_AUDIO before first recording


Push the branch:

git push -u origin feat/<short-task-name>


Open a PR against main:

gh pr create --base main --title "<title>" --body "<what changed + manual test steps for the A33>"


STOP. Do not merge. Wait for Peter to review, test on the device, and merge.

After Peter merges the PR:

Return to main and pull the merged code:

git checkout main
git pull origin main


Delete the now-merged local branch:

git branch -d feat/<old-task-name>


For the next task, start again at step 1 (a fresh branch off the updated main).

## Rules of thumb:

One branch per task. Do not pile unrelated changes onto one branch.
If a PR gets review feedback, push more commits to the SAME branch. Do not open
a new PR for the same task.
Never force-push, and never force-push to main. If you think a force-push is
needed, ask Peter first.
If the git state is unclear (detached HEAD, merge conflicts, unexpected
uncommitted changes), STOP and ask Peter instead of guessing.

Definition of done for a task

Compiles via ./gradlew assembleDebug.
PR is open against main with a clear description and explicit manual test
steps for the A33.
The branch is up to date with main (rebase or merge main in if it drifted).

## Important
I want the code to be modular easy to maintain and search for errors . I also want you to not take chanses , if you dont know something you check it with me and we can together solve the issue.

## Updates 
I dont want the app to look like ai slop , i want you to use this colors from a color scheme :#2C5EAD,#1591DC,#4BB8FA,#C4E2F5