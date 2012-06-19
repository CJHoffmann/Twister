#!/usr/bin/python

# File: CentralEngine.py ; This file is part of Twister.

# Copyright (C) 2012 , Luxoft

# Authors:
#    Andrei Costachi <acostachi@luxoft.com>
#    Andrei Toma <atoma@luxoft.com>
#    Cristian Constantin <crconstantin@luxoft.com>
#    Daniel Cioata <dcioata@luxoft.com>

# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at:

# http://www.apache.org/licenses/LICENSE-2.0

# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

'''
This file contains configurations for Central Engine:
  - Base path
  - Config path
If the file is executed with Python, it will start the Engine.
'''

import os
import sys
import cherrypy

TWISTER_PATH = os.getenv('TWISTER_PATH')
if not TWISTER_PATH:
    print('TWISTER_PATH environment variable is not set! Exiting!')
    exit(1)
sys.path.append(TWISTER_PATH)

from trd_party.BeautifulSoup import BeautifulStoneSoup
from server.centralengine.CentralEngineClasses import *

#

if __name__ == "__main__":

    # Read XML configuration file
    FMW_PATH = TWISTER_PATH + '/config/fwmconfig.xml'
    if not os.path.exists(FMW_PATH):
        logCritical("CE: Invalid path for config file: `%s` !" % FMW_PATH)
        exit(1)
    else:
        logDebug("CE: XML Config File: `%s`." % FMW_PATH)
        soup = BeautifulStoneSoup(open(FMW_PATH))
        serverPort = int(soup.centralengineport.text)
        del soup

    # Root path
    root = CentralEngine(FMW_PATH)

    # Config
    conf = {'global': {
        'server.socket_host': '0.0.0.0',
        'server.socket_port': serverPort,
        'server.thread_pool': 30,
        'engine.autoreload.on': False,
        'log.screen': False,
            }
        }

    # Start !
    cherrypy.quickstart(root, '/', config=conf)

#
