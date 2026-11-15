# Tilory

## Project
- Tilory (tile + story) is an Android puzzle game: rectangular tiles cut from the owner's own photos and drawings.
  Built for the owner first; maybe published on Google Play later.
- Stack: Kotlin + Jetpack Compose, native Android. Package `com.voronkova.tilory`.
- The game design lives in [docs/GAME_DESIGN.md](docs/GAME_DESIGN.md). Check it before changing game behaviour, and
  update it when a decision changes.
- Language: the owner chats in Russian. Everything in the repository and on GitHub (code, comments, docs, issues,
  commits) is in English.
- Claude manages the issues fully on its own: opens, comments, labels, closes and reopens them without asking the
  owner first.

# Work tracking through GitHub issues

## The idea
- The list of what is to do and what is done lives in the GitHub issues of `ekaterina-voronkova/tilory` that carry the
  `claude` label. Not in the chat, not in a TODO file: a new session with no chat history must be able to pick any
  work up from the issue.
- Claude keeps this list current on its own, without being asked.
- The owner talks in the chat. Claude turns what the owner says into issues and keeps them up to date.

## The cycle
1. **Start of every session.**
   - Run `gh issue list --label claude --state open`.
   - Read the issues that relate to the request before starting.
2. **Every remark or request from the owner becomes an issue**, opened before or as work starts.
   - A bug report, a wish, a screenshot with "this looks wrong": all of them become issues.
   - Skip one-off questions that need no change.
   - Ambiguous report: ask with concrete options before building, and write the chosen option into the issue.
3. **While working, comment on the issue** with what is worth keeping:
   - the cause found;
   - decisions and the options rejected;
   - what was measured;
   - what did not work.
4. **When done, close the issue** with a comment that:
   - names the commits;
   - says how the result was checked;
   - says what was not checked.

   Add the issue to the open release issue.
5. **A fix that turns out not to work: reopen the same issue** rather than opening a new one. The comment says why the
   first fix missed.

## Writing an issue
- Everything on GitHub is in one language (English), whatever language the chat is in.
- The body has two sections:
  - **Summary**: plain words for the owner, what and why. No code names unless they help.
  - **Context for Claude**: everything a new session needs:
    - files and functions;
    - causes and mechanisms;
    - test names;
    - commit hashes;
    - related issues.
- Title: the problem or the change as a short statement ("Command list: the scroll bar covers the shortcuts"), not
  "fix bug".
- Labels:
  - `claude` on everything Claude tracks;
  - `bug` or `enhancement` as fits;
  - `verify` (see below).

## Honesty about checking
- **Built but not checked in a real setup?** Close with the `verify` label, or open a follow-up issue that says exactly
  what is left to check. Examples: another Android version, a small or large screen, a release build, the owner's own
  phone.
- The closing comment says plainly what was and was not checked. "Tests pass" and "seen working in the app" are
  different claims.
- If the owner says a check is impossible for them, stop opening `verify` issues for it.

## Follow-ups
- Anything noticed along the way that is not part of the current task becomes its own issue right away, with enough
  context to start it later.
- Do not widen the current issue to absorb it.

## Releases
- One issue per release ("Release v1.2.3"), labelled `claude`. It lists the issues the release contains, and is closed
  with the link to the release, the tag's commit and the build run.
- Keep only the current release and the two before it on GitHub. After publishing a new release, delete the older
  ones (`gh release delete <tag> --yes`, without `--cleanup-tag`: the git tags stay, so the findings log and issues
  still point at real commits). Note the deleted releases in the new release issue. The owner asked for this; no need
  to ask again.
- A findings log in the repository (`docs/findings.md`, created with the first release) keeps the detailed release
  history:
  - what changed;
  - the causes;
  - the build runs.

  Issues are the index, the log is the detail.

## Reports that arrive as issues
- If the app files crash or performance reports as issues (their own labels, such as `crash` or `performance`), handle
  them like owner remarks:
  1. Analyse the report.
  2. Open a `claude` issue for each real problem it shows, including errors in the measuring itself.
  3. Close the report with a comment that links the issue.
- Reports from other users: check for duplicates first and close a duplicate with a link to the original.

## Privacy in a public repository
- This repository is public.
- Never write into issues, commits or notes the names of the owner's employer, work organisations, repositories or
  accounts, or personal paths and data. Photos from the owner's library never go into the repository.
- Masking: anything such text has to mention gets replaced with a neutral description.

## Commits
- Every commit message names its issue: `Short description (#123)`.
- Commits end with the session's Co-Authored-By line.
