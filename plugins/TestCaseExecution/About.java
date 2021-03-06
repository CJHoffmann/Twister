/*
File: About.java ; This file is part of Twister.
Version: 2.003

Copyright (C) 2012-2013 , Luxoft

Authors: Andrei Costachi <acostachi@luxoft.com>
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Font;

public class About extends JPanel{
    private JTextArea ta;
    
    public About(){
        setLayout(new BorderLayout());
        JPanel p = new JPanel(){
            public void paintComponent(Graphics g){
                super.paintComponent(g);
                g.drawImage(RunnerRepository.background, 260, 0, null);
                g.drawImage(RunnerRepository.logo, 0, 0, null);
                g.setFont(new Font("TimesRoman", Font.BOLD, 14));
                g.drawString("Twister Framework", 485, 130);
                g.drawString("V.: "+RunnerRepository.getVersion(), 525, 165);
                g.drawString("Build date: "+RunnerRepository.getBuildDate(), 478, 180);
                g.drawString(RunnerRepository.os, 478, 195);
                g.drawString(RunnerRepository.python, 478, 210);
            }
        };
        p.setLayout(null);
        
        ta = new JTextArea();
        ta.setBackground(p.getBackground());
        ta.setFont(new Font("TimesRoman", Font.BOLD, 14));
        ta.setBounds(0,120,230,120);
        ta.setEditable(false);
        ta.setBorder(null);
        ta.setText(RunnerRepository.logotxt);
        p.add(ta);
        p.setSize(new Dimension(730,380));
        p.setPreferredSize(new Dimension(730,380));
        p.setMinimumSize(new Dimension(730,380));
        p.setMaximumSize(new Dimension(730,380));
        add(p,BorderLayout.CENTER );
    }
}