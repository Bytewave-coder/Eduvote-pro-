with open('app/src/main/java/com/example/ui/navigation/EduVoteNavGraph.kt', 'r') as f:
    text = f.read()

if "initial_setup" in text:
    print("RESTORED")
else:
    print("MISSING")
