==============================================================================
 SceneCraft - Translation Contribution Guide
==============================================================================

Thank you for your interest in helping translate SceneCraft! This document
explains how to improve an existing translation or add support for a new
language. No programming knowledge is required, only a text editor and some
knowledge of the language you want to work on.


------------------------------------------------------------------------------
 1. SUPPORTED LANGUAGES
------------------------------------------------------------------------------

 File name     Language                 Status
 -----------   ----------------------   --------------------------------------
 en_US.lang    English (US)             Source language (reference)
 es_ES.lang    Spanish (Spain)          Complete, review welcome
 id_ID.lang    Indonesian               Complete, review welcome
 en_PT.lang    English (Pirate Speak)   Complete, humorous variant
 And other language, you can see at src/main/lang.

Most translations were produced with AI assistance and have NOT yet been
reviewed by native speakers. Corrections of any size are very welcome, from a
single typo to a full proofreading pass.


------------------------------------------------------------------------------
 2. WAYS TO CONTRIBUTE
------------------------------------------------------------------------------

 a) Fix a mistake       Typos, grammar, wrong terminology, unnatural phrasing.
 b) Review a language   Proofread an entire file and submit your corrections.
 c) Add a new language  Translate the full file into a language not listed.
 d) Report an issue     Tell us about a string that is unclear, truncated, or
                        does not fit in the interface.


------------------------------------------------------------------------------
 3. GETTING STARTED
------------------------------------------------------------------------------

 1. Fork or clone the repository: <REPOSITORY URL>
 2. Open the language folder:     <PATH TO LANG FOLDER>
    (for example: src/main/resources/assets/scenecraft/lang/)
 3. Copy en_US.lang as your reference. Always work from the English file,
    because it is the source of truth and is updated first.
 4. Edit an existing file, or create a new one for your language.
 5. Submit your changes (see section 8).

Use any plain text editor that supports UTF-8, such as VS Code, Notepad++,
Sublime Text, or Kate. Avoid word processors like Microsoft Word.


------------------------------------------------------------------------------
 4. FILE FORMAT RULES
------------------------------------------------------------------------------

Each line follows the format:

    key=translated text

Strict rules:

 - Encoding: UTF-8 without BOM. Special characters (accents, 360, etc.)
   must be saved as real characters, not escape codes.
 - Never translate or edit the KEY (the part before the first "=").
 - Only translate the VALUE (the part after the first "=").
 - Keep the same keys in the same order as en_US.lang, including blank
   lines and comment lines. This keeps files easy to compare.
 - Lines starting with "#" are comments. Leave them in English so every
   language file stays consistent.
 - Do not leave trailing spaces at the end of a line.
 - Every key in en_US.lang must exist in your file. A missing key will
   fall back to English in-game.
 - Do not add keys that do not exist in en_US.lang.

File names use the locale code format language_COUNTRY, where the language
is lowercase and the country is UPPERCASE. This matches Minecraft's own
locale codes and is case-sensitive on some systems, for example:

    es_ES.lang   id_ID.lang   de_DE.lang   fr_FR.lang   pt_BR.lang


------------------------------------------------------------------------------
 5. PLACEHOLDERS AND SPECIAL SEQUENCES (IMPORTANT)
------------------------------------------------------------------------------

Some strings contain placeholders that the game fills in at runtime. These
MUST be copied exactly. Breaking them can cause wrong text or crashes.

 Sequence      Meaning                              Example
 -----------   ----------------------------------   --------------------------
 %s            Text inserted by the game            Done: %s
 %1$s, %2$s    Numbered text, order may change      Replay "%1$s" was not saved
 %1$d, %2$d    Numbered whole number                Render %1$d videos
 %%d%%%%       A progress percentage (e.g. 45%)     Uploading: %%d%%%%
 \n            Line break                           Line one\nLine two

Guidelines:

 - Keep every placeholder that appears in the English string. Do not add,
   remove, or retype them.
 - You MAY move a numbered placeholder (%1$s, %2$s, ...) to a different
   position in the sentence if your grammar requires it. This is the reason
   they are numbered.
 - Keep "%%d%%%%" exactly as written, with all the percent signs.
 - Keep "\n" as the two characters backslash and n. Do not press Enter
   inside a value. Each entry must stay on ONE line.
 - Do not translate product or format names: YouTube, Blender, FFmpeg, MP4,
   WEBM, MKV, PNG, JPEG, OpenEXR, ODS, VeeR.tv, Oculus Rift, VR Player.


------------------------------------------------------------------------------
 6. STYLE AND TERMINOLOGY GUIDELINES
------------------------------------------------------------------------------

 - Tone: clear, friendly, and natural. Translate the meaning, not word for
   word. Write the way a native speaker would expect to read a game mod.
 - Consistency: pick one translation for each recurring term and use it
   everywhere. Key terms to keep consistent include:

       Keyframe, Replay, Render, Marker (Event Marker), Camera Path,
       Timeline, Spectator, Interpolator, Preset, Thumbnail, Bitrate,
       Framerate, Queue

 - Official Minecraft terms: where Minecraft already has an official
   translation for a word in your language (for example "Spectator",
   "Settings", "Night Vision"), use that official term so the mod feels
   native to the game.
 - Well-known loanwords: some languages commonly keep English technical
   terms (for example "keyframe" or "render"). This is acceptable if it is
   what players in your language actually use.
 - Register: use the form of address that fits your language's Minecraft
   community (formal or informal). Stay consistent within the file.
 - Length: GUI buttons and labels have limited space. Avoid translations
   much longer than the English text for short strings such as buttons,
   keybindings, and tooltips. If it does not fit, use a shorter wording.
 - Humor and special variants: joke or stylistic locales (such as Pirate
   Speak) may use playful wording, but must keep the meaning and every
   placeholder intact.
 - Keep original punctuation style appropriate for your language (for
   example, Spanish uses opening marks like "¿" and "¡").


------------------------------------------------------------------------------
 7. TESTING YOUR TRANSLATION
------------------------------------------------------------------------------

Before submitting, please check the following:

 1. The file opens as UTF-8 and special characters display correctly.
 2. The number of keys matches en_US.lang (no missing or extra entries).
 3. All placeholders (%s, %1$s, %1$d, %%d%%%%, \n) are present and unchanged
    in every line where the English text has them.
 4. Launch the game, select your language in Options > Language, and open
    these screens to check text fit and meaning:
      - Replay Viewer and Settings
      - Render Settings and Rendering screens
      - Edit Keyframe dialogs
      - In-game menu and keybinding list
      - Chat messages (start/stop recording, camera path)
 5. Look for text that is cut off, overlaps, or looks unnatural in context.


------------------------------------------------------------------------------
 8. SUBMITTING YOUR CHANGES
------------------------------------------------------------------------------

Option A - Pull Request (preferred)
 1. Create a branch named after your change, for example:
        translation/fix-id_ID   or   translation/add-de_DE
 2. Commit ONLY the language file(s) you changed.
 3. Write a clear commit message, for example:
        "Fix terminology consistency in id_ID.lang"
        "Add German (de_DE) translation"
 4. Open a Pull Request and briefly describe what you changed. If you are a
    native speaker, please mention it, as this helps reviewers.

Option B - Issue or direct submission
 If you are not comfortable with Git, open an issue at <ISSUES URL> and attach
 your file or paste your corrections, listing the key names you changed.
 A maintainer will merge them for you.

Please keep each Pull Request focused. One language per Pull Request makes
review faster.


------------------------------------------------------------------------------
 9. ADDING A NEW LANGUAGE
------------------------------------------------------------------------------

 1. Check the list in section 1 to make sure the language is not already
    present, and check the open Pull Requests to avoid duplicate work.
 2. Find the correct locale code (language lowercase, COUNTRY uppercase,
    for example de_DE) from the official Minecraft language list.
 3. Copy en_US.lang to <locale>.lang, for example de_DE.lang.
 4. Translate every value, following sections 4 to 6.
 5. Test in-game (section 7).
 6. Submit following section 8, and add your language to the table in
    section 1 of this README.

Partial translations are accepted, but please say so in your Pull Request.
Untranslated keys will be shown in English until completed.


------------------------------------------------------------------------------
 10. REVIEW PROCESS
------------------------------------------------------------------------------

 - A maintainer checks the file format, placeholders, and key coverage.
 - For languages that the maintainers do not speak, we rely on contributor
   feedback. If another contributor who speaks that language reviews your
   Pull Request, it will usually be merged faster.
 - If there are disagreements about terminology, the official Minecraft
   translation in that language takes priority, followed by what is most
   commonly used by the community.
 - We may ask for small changes before merging. Please do not be discouraged,
   this is only to keep quality consistent.


------------------------------------------------------------------------------
 11. KNOWN ISSUES (HELP WANTED)
------------------------------------------------------------------------------

 - cy_GB.lang (Welsh): the key
       scenecraft.gui.rendersettings.customresolution.warning.yuv420.cubic
   contains a wrong word ("ddiduedd"). It should read:
       Ar gyfer y rhagosodiad hwn, rhaid i'r uchder fod yn rhanadwy â 6
 - cy_GB.lang and la_LA.lang: need review by native or fluent speakers,
   especially long descriptions (renderer descriptions, keyframe warning).
   Welsh consonant mutations and Latin case/word order are the most likely
   sources of errors.
 - The source file has typos in comments and strings (for example
   "necessary", "occurred"). These do not affect functionality and may be
   fixed in a future update.


------------------------------------------------------------------------------
 12. CREDITS
------------------------------------------------------------------------------

Contributors are credited in CONTRIBUTORS.txt. When you submit a
translation, please include the name or handle you would like to be listed
under.

Translators:
  - <Name / Handle> - <Language>


------------------------------------------------------------------------------
 13. LICENSE
------------------------------------------------------------------------------

By submitting a translation, you agree that your contribution is released
under the same license as this project (see LICENSE).

Thank you for helping make SceneCraft accessible to more players!
==============================================================================
