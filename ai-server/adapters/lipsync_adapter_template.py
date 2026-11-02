"""Safe local lip-sync adapter template.
Configure FACELAB_LIPSYNC_CMD_JSON only after an exact local engine snapshot and
its weights have been license-reviewed. No remote API is called here.
"""
import argparse

def main():
    ap=argparse.ArgumentParser(); ap.add_argument('--video',required=True); ap.add_argument('--audio',required=True); ap.add_argument('--output',required=True); ap.parse_args()
    raise SystemExit('Template only: connect an audited local MuseTalk/other lip-sync implementation before use.')

if __name__=='__main__': main()
