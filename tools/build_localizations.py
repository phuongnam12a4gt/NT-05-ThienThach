"""Validate all 20 catalogs and generate Android resources. Run from repository root."""
from pathlib import Path
import json, re, xml.etree.ElementTree as ET
from collections import Counter
ROOT = Path(__file__).resolve().parents[1]
CATALOG = ROOT / "tools/localizations.json"
LANGUAGES = "vi en zh ja ko pt es hi id th ru de fr tr ar it pl fil ms ur".split()
SHARED = {"brand", "armor_value", "time_format"}
def load(): return json.loads(CATALOG.read_text(encoding="utf-8"))
def translated_keys(): return [key for key in load()["en"] if key not in SHARED]
def add(tag, block):
    data = load(); keys = translated_keys(); lines = block.strip().splitlines()
    assert len(keys) == len(lines), f"{tag}: expected {len(keys)} strings, got {len(lines)}"
    data[tag] = {**{key: data["en"][key] for key in SHARED}, **dict(zip(keys, lines))}
    CATALOG.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
def validate(data):
    assert set(data) == set(LANGUAGES), f"Missing languages: {set(LANGUAGES)-set(data)}"
    base = data["en"]
    for tag in LANGUAGES:
        assert set(data[tag]) == set(base), f"{tag}: mismatched keys"
        for key, value in data[tag].items():
            assert value.strip(), f"{tag}/{key}: empty translation"
            args = lambda text: Counter(re.findall(r"%\d+\$[0-9.]*[dsf]", text))
            assert args(value) == args(base[key]), f"{tag}/{key}: format mismatch"
    return len(base)
def android_text(value):
    return value.replace("'", "\\'").replace('"', '\\"')
def generate():
    data = load(); count = validate(data)
    res = ROOT / "app/src/main/res"
    for tag in LANGUAGES:
        # Indonesian uses Android's legacy resource qualifier; BCP-47 fil is unambiguous.
        folder = "values-in" if tag == "id" else "values-b+fil" if tag == "fil" else f"values-{tag}"
        locations = [res/folder]
        if tag == "en": locations += [res/"values"]
        for location in locations:
            location.mkdir(exist_ok=True)
            root = ET.Element("resources")
            for key in data["en"]:
                if key == "brand" and location.name != "values": continue
                attrs = {"name": key}
                if key == "brand": attrs["translatable"] = "false"
                if not re.search(r"%\d+\$", data[tag][key]): attrs["formatted"] = "false"
                ET.SubElement(root, "string", attrs).text = android_text(data[tag][key])
            tree = ET.ElementTree(root); ET.indent(tree)
            tree.write(location/"strings.xml", encoding="utf-8", xml_declaration=True)
    print(f"Validated and generated {len(LANGUAGES)} languages x {count} strings")
if __name__ == "__main__": generate()
