import re

with open('app/src/main/java/com/example/ui/Translator.kt', 'r') as f:
    text = f.read()

text = text.replace('"App Settings" to "ऐप सेटिंग्स",', '"App Settings" to "ऐप सेटिंग्स",\n        "App UI Design & Theme" to "ऐप UI डिज़ाइन और थीम",\n        "Select a custom layout theme. This will instantly change the appearance of the dashboard and voting screens." to "कस्टम लेआउट थीम चुनें। यह तुरंत डैशबोर्ड और वोटिंग स्क्रीन का रूप बदल देगा।",')

with open('app/src/main/java/com/example/ui/Translator.kt', 'w') as f:
    f.write(text)
