import re
with open('app/src/main/java/com/example/ui/navigation/EduVoteNavGraph.kt', 'r') as f:
    text = f.read()

text = text.replace('val startDestination = if (isBlocked) "blocked" else if (!isSetupComplete) "initial_setup" else "welcome"', 'val startDestination = if (isBlocked) "blocked" else "welcome"')
text = text.replace('val dest = if (isSetup) "welcome" else "initial_setup"', 'val dest = "welcome"')

with open('app/src/main/java/com/example/ui/navigation/EduVoteNavGraph.kt', 'w') as f:
    f.write(text)
