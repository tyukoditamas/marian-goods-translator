#!/usr/bin/env python3
import sys
import os
import traceback

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    sys.stderr.reconfigure(encoding="utf-8", errors="replace")

def main():
    try:
        import pandas as pd
        from transformers import MarianMTModel, MarianTokenizer
        import torch

        if len(sys.argv) < 2:
            print("Usage: translate_excel.py <input_excel>", flush=True)
            sys.exit(2)

        in_path = sys.argv[1]
        if not os.path.exists(in_path):
            print(f"Input file not found: {in_path}", flush=True)
            sys.exit(3)

        print(f"🔎 Reading: {in_path}", flush=True)
        df = pd.read_excel(in_path)

        colname = "descriere bunuri / Description of goods (limba engleza!)"
        if colname not in df.columns:
            print(f" Column not found: {colname}", flush=True)
            sys.exit(4)

        texts = df[colname].astype(str).fillna("").tolist()
        print(f"📋 Loaded {len(texts)} rows to translate.", flush=True)

        # Setup MarianMT
        print("Loading MarianMT model (en→ro)...", flush=True)
        model_name = 'Helsinki-NLP/opus-mt-en-ro'
        tokenizer = MarianTokenizer.from_pretrained(model_name)
        model = MarianMTModel.from_pretrained(model_name)
        model.eval()

        batch_size = 128
        translated = []
        for i in range(0, len(texts), batch_size):
            batch = texts[i:i+batch_size]
            inputs = tokenizer(
                batch,
                return_tensors="pt",
                padding=True,
                truncation=True,
                max_length=512
            )
            with torch.no_grad():
                outputs = model.generate(
                    **inputs,
                    num_beams=1,
                    early_stopping=True,
                    max_length=128
                )
            out_texts = [tokenizer.decode(t, skip_special_tokens=True) for t in outputs]
            translated.extend(out_texts)
            print(f"Translated rows {i+1} to {min(i+batch_size, len(texts))}", flush=True)

        # Replace column
        # Clean up translations for weird MarianMT output
        cleaned_translations = []
        for src, tgt in zip(texts, translated):
            # If input is a single word and output starts with that word, keep only the first word
            if len(src.split()) == 1 and tgt.lower().startswith(src.lower()):
                cleaned_translations.append(tgt.split()[0])
            else:
                # Remove any MarianMT parenthesis/notes
                cleaned = tgt.split('(')[0].split(',')[0].strip()
                cleaned_translations.append(cleaned)

        df[colname] = cleaned_translations

        # Output file
        out_path = (
                os.path.splitext(in_path)[0] + "_translated.xlsx"
        )
        df.to_excel(out_path, index=False)
        print(f"Wrote translated Excel: {out_path}", flush=True)
        print(out_path)  # Final line: output for Java code
        sys.exit(0)

    except Exception as ex:
        print("Exception occurred:", flush=True)
        traceback.print_exc()
        sys.exit(10)

if __name__ == "__main__":
    main()
