#!/usr/bin/env python3

from pathlib import Path
import glob
import os

os.chdir(os.path.dirname(os.path.abspath(__file__)))

title = lambda s: s.replace('_',' ').title().replace(' ','')

units = ['miner', 'landscaper', 'delivery_drone']
buildings = ['refinery', 'vaporator', 'design_school', 'fulfillment_center', 'net_gun', 'hq']

base = open("templates/base.java").read()
base = base.replace("/*{%SWITCH%}*/", ''.join((f"case {bot.upper()}: run{title(bot)}();break;\n") for bot in units+buildings))

for bot_type in ['units', 'buildings']:
	for bot in vars()[bot_type]:
		if not Path(f'{bot_type}/{bot}.java').is_file():
			with open(f'{bot_type}/{bot}.java', 'w') as f:
				f.write(f'static void run{title(bot)}() throws GameActionException {{\n\n}}\n')

base = base.replace("/*{%CODE%}*/", ''.join(open(fn).read() for fn in glob.glob('shared/*.java'))+''.join(''.join(open(f'{bot_type}/{bot}.java').read() for bot in globals()[bot_type]) for bot_type in ['units', 'buildings']))
with open('RobotPlayer.java', 'w') as f:
	f.write(base)
