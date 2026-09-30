"""APK-Hilfsskript (ersetzt zipalign aus dem Android-SDK).

  python3 package.py build <gelinkt.apk> <classes.dex> <out.apk>
      fügt classes.dex ein und richtet unkomprimierte Einträge auf 4 Byte aus
      (nötig u. a. für resources.arsc ab targetSdk 30)
  python3 package.py check <app.apk>
      prüft die Ausrichtung (z. B. nach dem Signieren)
"""
import sys
import zipfile

FIXED = (2008, 1, 1, 0, 0, 0)  # feste Zeitstempel → reproduzierbare Builds


def add(zout, name, data, ctype):
    zi = zipfile.ZipInfo(name, date_time=FIXED)
    zi.compress_type = ctype
    zi.external_attr = 0o644 << 16
    if ctype == zipfile.ZIP_STORED:
        start = zout.fp.tell() + 30 + len(name.encode('utf-8'))
        zi.extra = b'\0' * ((-start) % 4)
    zout.writestr(zi, data, compress_type=ctype, compresslevel=9 if ctype == zipfile.ZIP_DEFLATED else None)


def build(src, dex, dst):
    with zipfile.ZipFile(src) as zin, zipfile.ZipFile(dst, 'w') as zout:
        names = [i.filename for i in zin.infolist()]
        first = [n for n in names if n == 'AndroidManifest.xml']
        for n in first + [n for n in names if n not in first]:
            add(zout, n, zin.read(n), zin.getinfo(n).compress_type)
            if n == 'AndroidManifest.xml':
                with open(dex, 'rb') as f:
                    add(zout, 'classes.dex', f.read(), zipfile.ZIP_DEFLATED)


def misaligned(path):
    bad = []
    with zipfile.ZipFile(path) as z, open(path, 'rb') as f:
        for i in z.infolist():
            f.seek(i.header_offset + 26)
            n = int.from_bytes(f.read(2), 'little')
            e = int.from_bytes(f.read(2), 'little')
            if i.compress_type == zipfile.ZIP_STORED and (i.header_offset + 30 + n + e) % 4:
                bad.append(i.filename)
    return bad


if __name__ == '__main__':
    mode = sys.argv[1]
    if mode == 'build':
        build(*sys.argv[2:5])
        target = sys.argv[4]
    else:
        target = sys.argv[2]
    bad = misaligned(target)
    print(f'{target}: ' + ('Ausrichtung OK' if not bad else f'NICHT ausgerichtet: {bad}'))
    sys.exit(1 if bad else 0)
