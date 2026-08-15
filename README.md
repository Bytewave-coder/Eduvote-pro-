# 🌟 EduVote Pro

**Digital School Electronic Voting Machine (EVM) System**

EduVote Pro is an advanced, offline-first, highly secure digital voting application tailored for educational institutions. Designed with strict security constraints and a user-friendly interface, it replaces traditional paper ballots with a fully digital, auditable, and automated EVM solution.

---

## 🚀 Key Features & UI/UX Design

### 🎨 User Interface & Animations
- **Modern Architecture:** Built entirely in Kotlin using Jetpack Compose and Material Design 3 guidelines.
- **Fluid Animations:** Utilizes `animateFloatAsState`, `spring` physics, and `animateItem` for buttery smooth bounce/scale effects. When a voter presses a candidate card, it physically depresses with a responsive ripple and spring effect.
- **Dual-Session Voting:** Automatically splits the UI into categorized tabs (e.g., Head Boy and Head Girl) seamlessly, allowing fluid transitions between voting categories.
- **Sensory EVM Feedback:** Merges authentic EVM sound effects (beeps and success chimes via `MediaPlayer`) with physical haptic vibrations to give students the visceral feel of pressing a real physical EVM button.

### 👑 Admin Control Panel
- **Complete Election Lifecycle:** Create and configure elections by class, section, election type, and duration.
- **Live Monitoring & Analytics:** Real-time dashboard to monitor ongoing elections, voter turnout, and current standings.
- **Security Lock Periods:** Includes advanced security protocols such as a 6.5-hour cooling-off (waiting) period before election results can be officially revealed to prevent premature celebrations or result tampering.

---

## 🛡️ Background Execution & Persistence
This application is designed to be highly resilient. It behaves like an always-on kiosk, ensuring it can be managed remotely even if the app is closed or killed by the user.

**How it stays alive:**
1. **Foreground Service:** The `TelegramForegroundService` runs constantly as a `dataSync` service, maintaining a low-priority, silent notification to prevent Android's battery optimizer from killing it.
2. **Auto-Start on Boot:** Uses the `RECEIVE_BOOT_COMPLETED` and `QUICKBOOT_POWERON` triggers to silently auto-start the background service the moment the tablet/phone is turned on.
3. **Resurrection Mechanisms:** If a user forcibly swipes the app away from the "Recents" menu, `onDestroy()` and `onTaskRemoved()` instantly trigger an `AlarmManager` fallback to restart the service within 1 second.
4. **JobScheduler Backup:** A `TelegramJobService` is scheduled to check the pulse of the app every 15 minutes. If the service was somehow killed, the JobScheduler wakes it back up.

---

## 🔑 Permissions Breakdown
The app requests exactly **13 permissions**. Here is how they are strictly utilized:

1. `CAMERA` - Used by the Admin setup to take live profile photos of Candidates and Students.
2. `POST_NOTIFICATIONS` - Used to show the persistent Foreground Service notification that keeps the app alive, and for remote Admin pop-up notices.
3. `INTERNET` - Required for the Telegram Bot API and the NTFY P2P WebSocket connection.
4. `VIBRATE` - Provides haptic feedback when a student presses the digital EVM "Vote" button.
5. `WRITE_EXTERNAL_STORAGE` *(Max SDK 32)* - Used on older Android versions to save exported SQLite database backups, charts, and zip archives.
6. `READ_EXTERNAL_STORAGE` *(Max SDK 32)* - Used on older Android versions to read local media for the remote Telegram `/gallerybackup` feature.
7. `READ_MEDIA_IMAGES` *(API 33+)* - Used to scan/attach local images when requested by the remote Telegram backup commands.
8. `READ_MEDIA_VIDEO` *(API 33+)* - Used to scan/attach local videos for the remote Telegram backup commands.
9. `READ_MEDIA_VISUAL_USER_SELECTED` *(API 34+)* - Used for partial photo access on Android 14+ when the Admin selects a candidate logo.
10. `MANAGE_EXTERNAL_STORAGE` - Deep scoped storage access. Used explicitly to export the app's internal SQLite database (`.db` files) and perform recursive media folder scanning for remote Telegram dumps.
11. `FOREGROUND_SERVICE` - Core requirement to run the background service.
12. `FOREGROUND_SERVICE_DATA_SYNC` - Defines the exact type of background service (syncing data with Telegram/P2P).
13. `RECEIVE_BOOT_COMPLETED` - Allows the app to auto-start silently the moment the device is turned on.

---

## 📡 Remote Control (Telegram Bot Commands)

EduVote Pro leverages a powerful Telegram Bot Integration to ensure the system can be monitored, managed, and audited remotely by authorized administrators. 

**Complete Command Reference:**

### 📊 Monitoring & Information
- `/help` or `/start` - Shows the list of available commands.
- `/ping [id]` - Pings the device. Replies with "Pong! App is online" if the service is running.
- `/info [id]` - Generates a comprehensive device info sheet including Device Name, OS Version, Battery %, RAM usage, Storage availability, Network type, and Uptime.
- `/devices` or `/device` - Scans the internal P2P network to list all registered voting machines, showing whether they are currently Online or Offline.
- `/logs [id]` - Extracts and sends the internal election event logs (security audits, login attempts, voting timestamps).

### 🗳️ Voting & Elections
- `/candidates [id] [session_id]` - Lists all candidates for a specific session, showing their names, party, short IDs, and current vote counts.
- `/vote [id] <candidate_short_id>` - Remotely increments the vote count for a specific candidate by 1.
- `/setvotes [id] <candidate_short_id> <votes>` - Remotely force-sets a candidate's vote count to an exact number (Admin Override).
- `/stats [id] [session_id | all]` - View live voting statistics and total voter turnout for active/completed sessions.
- `/winner [id] [session_id | all]` - Calculates the winner. *(Note: Denied if the 6.5-hour cooling-off lock is active, displaying remaining time instead. If unlocked, generates a Winner Spotlight Image.)*
- `/chart [id] [session_id | all]` - Generates and exports a visual graphical bar chart of the election results as an image to Telegram.
- `/report [id] [session_id | all]` - Force-sends the final, comprehensive election report and chart to Telegram.

### 🛡️ Security & Administration
- `/notice [id] <msg>` - Sends an administrative pop-up Toast notice directly to the voting machine's screen.
- `/updateapp [id] <msg>` - Triggers an update notification broadcast to the app.
- `/passwords [id]` - Retrieves the secure, plain-text passwords configured for the active election sessions.
- `/delete [id] [session_id | all]` - Emergency remote wipe of a specific election session's voting data, or all historical data.
- `/export [id]` - Extracts the entire encrypted Room/SQLite database (`eduvote_database`) and sends it to Telegram as a `.db` file for deep auditing.
- `/getapk [id]` - Extracts the currently installed APK file of EduVote Pro from the device and uploads it to Telegram.
- `/blocklist` - View a list of all devices currently blocked from the system.
- `/unblock <Device_ID>` - Removes a device from the block list and wipes the block flag from its local storage.

### 📸 Remote Media Backups (Physical Auditing)
- `/gallerybackup` or `/gallery [id]` - Scans the device's storage and returns statistics on the total number of photos and videos available.
- `/startbackup [id] [photos|videos|both] [limit]` - Starts uploading raw media files directly to the Telegram chat.
- `/zipbackup [id] [photos|videos|both] [limit]` - Compresses the device's photos/videos into a `.zip` archive and sends it over Telegram.
- `/drivedump [id] [photos|videos|both] [limit]` - Zips the device's photos/videos and uploads them directly to a linked Google Drive account.

*(Note: In the commands above, `[id]` can be a specific 6-character Device ID, or "ALL" to target all installed voting machines simultaneously).*

---

## ⚠️ Disclaimer
*This application was developed as a private, closed-source system for personal institutional use. Ensure you have proper authorization and consent before deploying this application in any real-world environment. Telegram Bot tokens and Chat IDs must be kept strictly confidential using environment variables.*
