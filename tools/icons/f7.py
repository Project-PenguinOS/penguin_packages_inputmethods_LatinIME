#!/usr/bin/env python3
import os
import re
import sys

src, out = sys.argv[1], sys.argv[2]
for n in sys.argv[3:]:
    s = open(os.path.join(src, n + '.svg')).read()
    vb = re.search(r'viewBox="([^"]+)"', s).group(1).split()
    paths = re.findall(r'<path[^>]*\sd="([^"]+)"', s)
    body = '\n'.join(
        f'    <path\n        android:fillColor="@android:color/white"\n'
        f'        android:pathData="{" ".join(d.split())}" />' for d in paths)
    with open(os.path.join(out, 'ic_f7_' + n + '.xml'), 'w') as f:
        f.write(f'''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="{vb[2]}"
    android:viewportHeight="{vb[3]}">
{body}
</vector>
''')
