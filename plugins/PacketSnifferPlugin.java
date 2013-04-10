/*
File: PacketSnifferPlugin.java ; This file is part of Twister.
Version: 2.001

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

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.border.BevelBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.twister.Item;
import com.twister.plugin.baseplugin.BasePlugin;
import com.twister.plugin.twisterinterface.TwisterPluginInterface;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.w3c.dom.Document;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class PacketSnifferPlugin extends BasePlugin implements
		TwisterPluginInterface {
	private static final long serialVersionUID = 1L;
	private JPanel p;
	public XmlRpcClient client;
	private ChannelSftp c;
	private String index = "0";
	// private JLabel sniff, reset;
	private JTable jTable1;
	private int length = 40000;
	private byte start = 0;// 0 pause,1 sniff;
	private JTextArea content;
	private ImageIcon button3, button0;
	private JCheckBox pro, srcip, srcmac, srcport, dstip, dstmac, dstport;
	private TableColumn[] columns = new TableColumn[7];
	private boolean go;
	private JButton bstart, breset;

	public static void main(String[] args) {
		try {
			XmlRpcClientConfigImpl configuration = new XmlRpcClientConfigImpl();
			configuration.setServerURL(new URL("http://11.126.32.9:8000/"));
			XmlRpcClient client = new XmlRpcClient();
			client.setConfig(configuration);
			final PacketSnifferPlugin sch = new PacketSnifferPlugin();
			sch.setRPC(client);
			sch.init(null, null, null, null);
			JFrame f = new JFrame();
			f.add(sch.getContent());
			f.setVisible(true);
			f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			f.setBounds(100, 50, 900, 900);
			System.out.println("Client initialized: " + client);
		} catch (Exception e) {
			System.out.println("Could not conect to "
					+ "http://tsc-server:8000 for RPC client initialization");
			e.printStackTrace();
		}
	}

	public void setRPC(XmlRpcClient client) {
		this.client = client;
	}

	@Override
	public void init(ArrayList<Item> suite, ArrayList<Item> suitetest,
			final Hashtable<String, String> variables,
			final Document pluginsConfig) {
		super.init(suite, suitetest, variables, pluginsConfig);
		System.out.println("Initializing " + getName() + " ...");
		p = new JPanel();
		initializeSFTP();
		initializeRPC();
		try{
			createXMLStructure();
		} catch (Exception e){
			System.out.println("Could not create plugin configuration from plugins XML");
			e.printStackTrace();
		}
		initializeMainPanel();
		try {
			length = Integer.parseInt(getPropValue("historyLength")
					.getNodeValue());
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			String status = client.execute("runPlugin",
					new Object[] { variables.get("user"), getName(),
							// String status = client.execute("runPlugin", new
							// Object[] {"tscguest", getName(),
							"command=echo" }).toString();
			System.out.println("status: " + status);
			if (status.equals("running")) {
				start = 1;
				// sniff.setIcon(button3);
				bstart.setText("Stop");
			}
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		go = true;
		new Thread() {
			public void run() {
				while (go) {
					// if(start==1)
					updateTable("command=query&data=" + index);
					try {
						Thread.sleep(1000);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}.start();
		System.out.println(getName() + " initialized");
	}

	/*
	 * initialize main content
	 */
	public void initializeMainPanel() {
		try {

			bstart = new JButton("Start");
			breset = new JButton("Reset");

			JPanel panel1 = new JPanel();
			panel1.setLayout(null);

			panel1.setMaximumSize(new Dimension(1150, 650));
			panel1.setMinimumSize(new Dimension(1150, 650));
			panel1.setPreferredSize(new Dimension(1150, 650));
			panel1.setSize(new Dimension(1150, 650));

			InputStream in = getClass().getResourceAsStream("Sniff0.png");
			Image im = ImageIO.read(in);
			button0 = new ImageIcon(im);
			in = getClass().getResourceAsStream("Sniff1.png");
			im = ImageIO.read(in);
			final ImageIcon button1 = new ImageIcon(im);
			in = getClass().getResourceAsStream("Stop1.png");
			im = ImageIO.read(in);
			final ImageIcon button2 = new ImageIcon(im);
			in = getClass().getResourceAsStream("Stop0.png");
			im = ImageIO.read(in);
			button3 = new ImageIcon(im);
			// in = getClass().getResourceAsStream("Reset0.png");
			// im = ImageIO.read(in);
			// final ImageIcon button4 = new ImageIcon(im);
			in = getClass().getResourceAsStream("Reset1.png");
			im = ImageIO.read(in);
			final ImageIcon button5 = new ImageIcon(im);
			in = getClass().getResourceAsStream("Save.png");
			im = ImageIO.read(in);
			ImageIcon save = new ImageIcon(im);
			// reset = new JLabel(button4);
			// reset.setBounds(543, 610, 120, 120);
			// panel1.add(reset);

			breset.setBounds(623, 610, 120, 30);
			panel1.add(breset);
			breset.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent arg0) {
					try {
						String resp = client.execute(
								"runPlugin",
								new Object[] { variables.get("user"),
										getName(), "command=reset" })
								.toString();
						((DefaultTableModel) jTable1.getModel()).setNumRows(0);
						index = "0";
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});

			// reset.addMouseListener(new MouseAdapter() {
			// public void mouseEntered(MouseEvent ev) {
			// reset.setCursor(Cursor
			// .getPredefinedCursor(Cursor.HAND_CURSOR));
			// }
			// public void mousePressed(MouseEvent ev) {
			// reset.setIcon(button5);
			// }
			// public void mouseReleased(MouseEvent ev) {
			// reset.setIcon(button4);
			// try{String resp = client.execute("runPlugin", new Object[] {
			// variables.get("user"), getName(),
			// "command=reset"}).toString();
			// ((DefaultTableModel)jTable1.getModel()).setNumRows(0);
			// index = "0";
			// System.out.println("resp: "+resp);
			// } catch(Exception e){
			// e.printStackTrace();
			// }
			// }
			// });

			bstart = new JButton("Start");
			bstart.setBounds(453, 610, 120, 30);
			panel1.add(bstart);
			bstart.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					if (start == 1) {
						bstart.setText("Start");
						start = 0;
						try {
							String resp = client.execute(
									"runPlugin",
									new Object[] { variables.get("user"),
											getName(),
											// String resp =
											// client.execute("runPlugin", new
											// Object[] { "tscguest", getName(),
											"command=pause" }).toString();
							System.out.println("resp: " + resp);
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					} else {
						bstart.setText("Stop");
						start = 1;
						try {
							String resp = client.execute(
									"runPlugin",
									new Object[] { variables.get("user"),
											getName(), "command=resume" })
									.toString();
							System.out.println("resp: " + resp);
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
				}
			});

			// sniff = new JLabel(button0);
			// sniff.setBounds(373, 610, 120, 120);
			// panel1.add(sniff);
			// sniff.addMouseListener(new MouseAdapter() {
			// public void mouseEntered(MouseEvent ev) {
			// sniff.setCursor(Cursor
			// .getPredefinedCursor(Cursor.HAND_CURSOR));
			// }
			// public void mousePressed(MouseEvent ev) {
			// if(start==0){
			// sniff.setIcon(button1);
			// } else {
			// sniff.setIcon(button2);
			// }
			// }
			// public void mouseReleased(MouseEvent ev) {
			// if(start==1){
			// sniff.setIcon(button0);
			// start=0;
			// try{
			// String resp = client.execute("runPlugin", new Object[] {
			// variables.get("user"), getName(),
			// //String resp = client.execute("runPlugin", new Object[] {
			// "tscguest", getName(),
			// "command=pause"}).toString();
			// System.out.println("resp: "+resp);
			// } catch(Exception e){
			// e.printStackTrace();
			// }
			// } else {
			// sniff.setIcon(button3);
			// start=1;
			// try{
			// String resp = client.execute("runPlugin", new Object[] {
			// variables.get("user"), getName(),
			// "command=resume"}).toString();
			// System.out.println("resp: "+resp);
			// } catch(Exception e){
			// e.printStackTrace();
			// }
			// }
			// }
			// });

			jTable1 = new JTable();
			jTable1.getTableHeader().setReorderingAllowed(false);
			jTable1.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			jTable1.getSelectionModel().addListSelectionListener(
					new ListSelectionListener() {
						@Override
						public void valueChanged(ListSelectionEvent arg0) {
							if (!arg0.getValueIsAdjusting()) {
								if (jTable1.getSelectedRow() == -1) {
									content.setText("");
									return;
								}
								String pac = null;
								try {
									String id = jTable1.getValueAt(
											jTable1.getSelectedRow(), 7)
											.toString();
									pac = client.execute(
											"runPlugin",
											new Object[] {
													variables.get("user"),
													getName(),
													"command=querypkt&data="
															+ id }).toString();
									JsonElement el = new JsonParser()
											.parse(pac);
									JsonObject jobject = el.getAsJsonObject();
									Gson gson = new GsonBuilder()
											.setPrettyPrinting().create();
									String json = gson.toJson(jobject);
									content.setText(json);
								} catch (Exception e) {
									System.out.println("Server response: "
											+ pac.toString());
									e.printStackTrace();

								}
							}
						}
					});

			JScrollPane pane = new JScrollPane(jTable1);
			pane.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
			pane.setBounds(25, 45, 1100, 300);

			jTable1.setModel(new DefaultTableModel(new Object[][] {},
					new String[] { "Protocol", "SRC IP", "SRC Mac", "SRC Port",
							"DST IP", "DST Mac", "DST Port", "" }) {
				private static final long serialVersionUID = 1L;

				public boolean isCellEditable(int row, int col) {
					return false;
				}
			});
			TableRowSorter sorter = new TableRowSorter(jTable1.getModel());
			jTable1.setRowSorter(sorter);
			TableColumn c = jTable1.getColumnModel().getColumn(7);
			c.setMinWidth(0);
			c.setMaxWidth(0);
			c.setWidth(0);
			c.setPreferredWidth(0);
			panel1.add(pane);

			content = new JTextArea();
			content.setEditable(false);
			JScrollPane sc = new JScrollPane(content);
			sc.setBounds(25, 350, 1100, 250);
			panel1.add(sc);

			JPanel top = new JPanel();
			top.setBounds(25,0,1100,40);
			panel1.add(top);
			
			JLabel filter = new JLabel("Filter:");
			//filter.setBounds(50, 10, 40, 25);
			
			panel1.add(top);
			//panel1.add(filter);
			top.add(filter);
			final JTextField search = new JTextField();
			search.setMinimumSize(new Dimension(130,20));
			search.setPreferredSize(new Dimension(130,20));
			search.addKeyListener(new KeyAdapter() {
				public void keyReleased(KeyEvent ev) {
					if (ev.getKeyCode() == KeyEvent.VK_ENTER) {
						filter(search.getText());
					}
				}
			});
			//panel1.add(search);
			top.add(search);

			filter = new JLabel("Start opt.:");
			//filter.setBounds(240, 10, 40, 25);

			//panel1.add(filter);
			top.add(filter);
			final JTextField search2 = new JTextField();
			//search2.setBounds(275, 10, 150, 25);
			search2.setMinimumSize(new Dimension(130,20));
			search2.setPreferredSize(new Dimension(130,20));
			search2.addKeyListener(new KeyAdapter() {
				public void keyReleased(KeyEvent ev) {
					if (ev.getKeyCode() == KeyEvent.VK_ENTER) {
						try {
							// String resp = client.execute("runPlugin", new
							// Object[] { "tscguest", getName(),
							String resp = client.execute(
									"runPlugin",
									new Object[] {
											variables.get("user"),
											getName(),
											"command=setfilters&data="
													+ search2.getText() })
									.toString();
							System.out.println(resp);
							((DefaultTableModel) jTable1.getModel())
									.setNumRows(0);
							index = "0";
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			});
			try {
				// HashMap<String, String> filters = (HashMap<String,
				// String>)client.execute("runPlugin", new Object[] {"tscguest",
				// getName(),
				HashMap<String, String> filters = (HashMap<String, String>) client
						.execute("runPlugin",
								new Object[] { variables.get("user"),
										getName(), "command=getfilters" });
				Iterator<Entry<String, String>> iter = filters.entrySet()
						.iterator();
				StringBuilder sb = new StringBuilder();
				Entry<String, String> en;
				while (iter.hasNext()) {
					en = iter.next();
					sb.append(en.getKey());
					sb.append(" ");
					sb.append(en.getValue());
					sb.append(" ");
				}
				System.out.println("Filter:" + sb.toString());
				search2.setText(sb.toString());
			} catch (Exception e) {
				e.printStackTrace();
			}
			//panel1.add(search2);
			top.add(search2);

			pro = new JCheckBox("Protocol");
			pro.setSelected(true);
			pro.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					if (pro.isSelected()) {
						jTable1.addColumn(columns[0]);
						jTable1.moveColumn(jTable1.getColumnCount() - 1, 0);
					} else {
						int index = findColumn("Protocol");
						columns[0] = jTable1.getColumnModel().getColumn(index);
						jTable1.removeColumn(columns[0]);
					}
				}
			});
			//pro.setBounds(425, 10, 75, 25);
			//panel1.add(pro);
			top.add(pro);
			srcip = new JCheckBox("SRC IP");
			srcip.setSelected(true);
			srcip.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					if (srcip.isSelected()) {
						int index = findColumn("Protocol");
						if (index == -1) {
							jTable1.addColumn(columns[1]);
							jTable1.moveColumn(jTable1.getColumnCount() - 1, 0);
						} else {
							jTable1.addColumn(columns[1]);
							jTable1.moveColumn(jTable1.getColumnCount() - 1, 1);
						}
					} else {
						int index = findColumn("SRC IP");
						columns[1] = jTable1.getColumnModel().getColumn(index);
						jTable1.removeColumn(columns[1]);
					}
				}
			});
			//srcip.setBounds(500, 10, 65, 25);
			//panel1.add(srcip);
			top.add(srcip);
			srcmac = new JCheckBox("SRC Mac");
			srcmac.setSelected(true);
			srcmac.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					if (srcmac.isSelected()) {
						jTable1.addColumn(columns[2]);
						int index = findColumn("SRC IP");
						if (index != -1) {
							jTable1.moveColumn(jTable1.getColumnCount() - 1,
									index + 1);
						} else if (findColumn("Protocol") != -1) {
							jTable1.moveColumn(jTable1.getColumnCount() - 1, 1);
						} else {
							jTable1.moveColumn(jTable1.getColumnCount() - 1, 0);
						}
					} else {
						int index = findColumn("SRC Mac");
						columns[2] = jTable1.getColumnModel().getColumn(index);
						jTable1.removeColumn(columns[2]);
					}
				}
			});
			//srcmac.setBounds(565, 10, 80, 25);
			//panel1.add(srcmac);
			top.add(srcmac);
			srcport = new JCheckBox("SRC Port");
			srcport.setSelected(true);
			srcport.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					if (srcport.isSelected()) {
						jTable1.addColumn(columns[3]);
						int index = findColumn("SRC Mac");
						if (index != -1) {
							jTable1.moveColumn(jTable1.getColumnCount() - 1,
									index + 1);
						} else {
							index = findColumn("SRC IP");
							if (index != -1) {
								jTable1.moveColumn(
										jTable1.getColumnCount() - 1, index + 1);
							} else if (findColumn("Protocol") != -1) {
								jTable1.moveColumn(
										jTable1.getColumnCount() - 1, 1);
							} else {
								jTable1.moveColumn(
										jTable1.getColumnCount() - 1, 0);
							}
						}
					} else {
						int index = findColumn("SRC Port");
						columns[3] = jTable1.getColumnModel().getColumn(index);
						jTable1.removeColumn(columns[3]);
					}
				}
			});
			//srcport.setBounds(645, 10, 80, 25);
			//panel1.add(srcport);
			top.add(srcport);
			dstip = new JCheckBox("DST IP");
			dstip.setSelected(true);
			dstip.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					if (dstip.isSelected()) {
						jTable1.addColumn(columns[4]);
						int index = findColumn("DST Mac");
						if (index != -1) {
							jTable1.moveColumn(jTable1.getColumnCount() - 1,
									index);
						} else {
							index = findColumn("DST Port");
							if (index != -1) {
								jTable1.moveColumn(
										jTable1.getColumnCount() - 1, index);
							}
						}
					} else {
						int index = findColumn("DST IP");
						columns[4] = jTable1.getColumnModel().getColumn(index);
						jTable1.removeColumn(columns[4]);
					}
				}
			});
			//dstip.setBounds(725, 10, 65, 25);
			//panel1.add(dstip);
			top.add(dstip);
			dstmac = new JCheckBox("DST Mac");
			dstmac.setSelected(true);
			dstmac.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					if (dstmac.isSelected()) {
						jTable1.addColumn(columns[5]);
						int index = findColumn("DST Port");
						if (index != -1) {
							jTable1.moveColumn(jTable1.getColumnCount() - 1,
									index);
						}
					} else {
						int index = findColumn("DST Mac");
						columns[5] = jTable1.getColumnModel().getColumn(index);
						jTable1.removeColumn(columns[5]);
					}
				}
			});
			//dstmac.setBounds(790, 10, 75, 25);
			//panel1.add(dstmac);
			top.add(dstmac);
			dstport = new JCheckBox("DST Port");
			dstport.setSelected(true);
			dstport.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					if (dstport.isSelected()) {
						jTable1.addColumn(columns[6]);
					} else {
						int index = findColumn("DST Port");
						columns[6] = jTable1.getColumnModel().getColumn(index);
						jTable1.removeColumn(columns[6]);
					}
				}
			});
			//dstport.setBounds(865, 10, 80, 25);
			//panel1.add(dstport);
			top.add(dstport);
			// JButton load = new JButton(open);
			// load.addActionListener(new ActionListener() {
			// @Override
			// public void actionPerformed(ActionEvent arg0) {
			// loadFile();
			// }
			// });
			// load.setBounds(830,5,35,35);
			// panel1.add(load);
			JButton bsave = new JButton(save);
			bsave.setPreferredSize(new Dimension(35,35));
			bsave.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					saveFile();
				}
			});
			//bsave.setBounds(950, 5, 35, 35);
			//panel1.add(bsave);
			top.add(bsave);
			p.add(new JScrollPane(panel1));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*
	 * method to load the content of a file from local pc to server and into the
	 * table
	 */
	public void loadFile() {
		JFileChooser chooser = new JFileChooser();
		int val = chooser.showDialog(p, "Select");
		if (val == JFileChooser.APPROVE_OPTION) {
			System.out.println("file is: "
					+ chooser.getSelectedFile().getAbsolutePath());
		}
	}

	/*
	 * method to save the content of the table to a file
	 */
	public void saveFile() {
		try {
			JFileChooser chooser = new JFileChooser();
			int val = chooser.showDialog(p, "Select");
			if (val == JFileChooser.APPROVE_OPTION) {
				String file = chooser.getSelectedFile().getAbsolutePath();
				String location = client.execute(
						"runPlugin",
						new Object[] { variables.get("user"), getName(),
								"command=savepcap" }).toString();
				InputStream in = c.get(location);
				OutputStream out = new FileOutputStream(
						new File(file + ".pcap"));
				int read = 0;
				byte[] bytes = new byte[1024];
				while ((read = in.read(bytes)) != -1) {
					out.write(bytes, 0, read);
				}
				in.close();
				out.flush();
				out.close();
				System.out.println("location: " + location);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * search column by name and return the index
	 */
	private int findColumn(String name) {
		for (int i = 0; i < jTable1.getColumnCount(); i++) {
			if (jTable1.getColumnName(i).toLowerCase()
					.equals(name.toLowerCase())) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public Component getContent() {
		return p;
	}

	@Override
	public String getDescription() {
		String description = "Packets Twist (SNIFF) is a Twister plugin that captures "+
							 "network packets and list them to users, allowing them to "+
							 "filter the packets as they are captured or after capture."+
							 " All in all this plugin has a functionality similar to Wireshark.";
		return description;
	}

	@Override
	public String getFileName() {
		String filename = "PacketSnifferPlugin.jar";
		return filename;
	}

	@Override
	public void terminate() {
		super.terminate();
		p = null;
		client = null;
		c = null;
		go = false;
		index = null;
		bstart = null;
		breset = null;
		jTable1 = null;
		content = null;
		button3 = null;
		button0 = null;
		pro = null;
		srcip = null;
		srcmac = null;
		srcport = null;
		dstip = null;
		dstmac = null;
		dstport = null;
		columns = null;

	}

	@Override
	public String getName() {
		String name = "PacketSnifferPlugin";
		return name;
	}

	/*
	 * filter table based on filter values
	 * column_name=value&column_name2=value2&...
	 */
	public void filter(String filter) {
		if (filter.indexOf("=") == -1 && filter.indexOf("&") == -1) {
			RowFilter rf = null;
			try {
				int nr = jTable1.getColumnCount() - 1;
				int[] index = new int[nr];
				for (int i = 0; i < nr; i++) {
					index[i] = i;
				}
				rf = RowFilter.regexFilter(filter, index);
				((TableRowSorter) jTable1.getRowSorter()).setRowFilter(rf);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (filter.indexOf("&") != -1) {
			String[] re = filter.split("&");
			ArrayList list = new ArrayList();
			for (String con : re) {
				String column = con.split("=")[0];
				String val = con.split("=")[1];
				int col = findColumn(column);
				RowFilter rf = RowFilter.regexFilter(val, col);
				list.add(rf);
			}
			((TableRowSorter) jTable1.getRowSorter()).setRowFilter(RowFilter
					.andFilter(list));
		} else {
			String column = filter.split("=")[0];
			String val = filter.split("=")[1];
			int col = findColumn(column);
			RowFilter rf = RowFilter.regexFilter(val, col);
			((TableRowSorter) jTable1.getRowSorter()).setRowFilter(rf);
		}
		jTable1.clearSelection();
	}

	/*
	 * initialize main SFTP connection
	 */
	public void initializeSFTP() {
		try {
			JSch jsch = new JSch();
			String user = variables.get("user");
			// String user = "tscguest";
			Session session = jsch.getSession(user, variables.get("host"), 22);
			// Session session = jsch.getSession(user, "tsc-server", 22);
			session.setPassword(variables.get("password"));
			// session.setPassword("tscguest");
			Properties config = new Properties();
			config.put("StrictHostKeyChecking", "no");
			session.setConfig(config);
			session.connect();
			Channel channel = session.openChannel("sftp");
			channel.connect();
			c = (ChannelSftp) channel;
			System.out.println("SFTP successfully initialized");
		} catch (Exception e) {
			System.out.println("SFTP could not be initialized");
			e.printStackTrace();
		}
	}

	public void updateTable(String command) {
		HashMap<String, HashMap> hash = null;
		try {
			DefaultTableModel model = ((DefaultTableModel) jTable1.getModel());
			hash = (HashMap<String, HashMap>) client.execute("runPlugin",
					new Object[] { variables.get("user"), getName(),
							"command=query&data=" + index });
			// .execute("runPlugin", new Object[] { variables.get("user"),
			// getName(),command});
			// System.out.println("hash: " + hash.toString()+" index: "+ index);
			// System.out.println("hash: " + hash.toString()+" index: "+ index);
			// HashMap<String, HashMap[]> hash2 = hash.get("data");
			HashMap hash2 = hash.get("data");
			// System.out.println("hash2: "+hash2.toString());
			index = hash2.get("id").toString();
			// System.out.println("index: " + index);
			Object[] hash3 = (Object[]) hash2.get("packets");
			// System.out.println("hash3: "+hash3.toString()+" - "+hash3.length);
			String protocol, sip, smac, sport, dip, dmac, dport, id = null;
			for (Object ob : hash3) {
				try {
					// System.out.println(ob.toString());
					JsonElement el = new JsonParser().parse(ob.toString());
					JsonObject jobject = el.getAsJsonObject();
					// JsonElement pael = jobject.get("packet");
					// jobject = pael.getAsJsonObject();
					// String packet = jobject.toString();
					// System.out.println("packet: "+packet);

					// Gson gson = new
					// GsonBuilder().setPrettyPrinting().create();
					// String json = gson.toJson(jobject);
					// System.out.println("packet: "+json);

					// jobject = el.getAsJsonObject();

					el = jobject.get("packet_head");
					jobject = el.getAsJsonObject();

					JsonElement source = jobject.get("source");
					jobject = source.getAsJsonObject();
					source = jobject.get("ip");
					sip = source.toString();
					sip = sip.substring(1, sip.length() - 1);
					// System.out.println("source:"+source.toString());
					source = jobject.get("mac");
					smac = source.toString();
					smac = smac.substring(1, smac.length() - 1);
					// System.out.println("mac:"+source.toString());
					source = jobject.get("port");
					sport = source.toString();
					sport = sport.substring(1, sport.length() - 1);
					// System.out.println("port:"+source.toString());

					jobject = el.getAsJsonObject();
					source = jobject.get("destination");
					jobject = source.getAsJsonObject();
					source = jobject.get("ip");
					dip = source.toString();
					dip = dip.substring(1, dip.length() - 1);
					// System.out.println("source:"+source.toString());
					source = jobject.get("mac");
					dmac = source.toString();
					dmac = dmac.substring(1, dmac.length() - 1);
					// System.out.println("mac:"+source.toString());
					source = jobject.get("port");
					dport = source.toString();
					dport = dport.substring(1, dport.length() - 1);
					// System.out.println("port:"+source.toString());

					jobject = el.getAsJsonObject();
					source = jobject.get("protocol");
					protocol = source.toString();
					protocol = protocol.substring(1, protocol.length() - 1);
					source = jobject.get("id");
					id = source.toString();
					id = id.substring(1, id.length() - 1);
					// System.out.println("protocol:"+source.toString());
					model.addRow(new Object[] { protocol, sip, smac, sport,
							dip, dmac, dport, id });
				} catch (Exception e) {
					System.out.println("Server response: " + hash.toString());
					e.printStackTrace();
				}
			}
			if (model.getRowCount() > length) {
				int delete = model.getRowCount() - length;
				for (int i = 0; i < delete; i++) {
					model.removeRow(0);
				}
			}
		} catch (Exception e) {
			System.out.println("Server response: " + hash.toString());
			e.printStackTrace();
		}

	}

	/*
	 * initialize main RPC connection
	 */
	public void initializeRPC() {
		try {
			XmlRpcClientConfigImpl configuration = new XmlRpcClientConfigImpl();
			configuration.setServerURL(new URL("http://"
					+ variables.get("host") + ":"
					+ variables.get("centralengineport")));

			// configuration.setServerURL(new URL("http://tsc-server:88/"));
			client = new XmlRpcClient();
			client.setConfig(configuration);
			System.out.println("Client initialized: " + client);
		} catch (Exception e) {
			if(variables!=null && 
			   variables.get("host")!=null &&
			   variables.get("centralengineport")!=null){
				System.out.println("Could not conect to " + variables.get("host")
						+ variables.get("centralengineport"));
			} else {
				System.out.println("Could not connect and initialize RPC connection to server."+
								   "Configuration variables might not be properly set.");
			}
			
		}
	}

	/*
	 * method to copy plugins configuration file to server
	 */
	public boolean uploadPluginsFile() {
		try {
			DOMSource source = new DOMSource(pluginsConfig);
			File file = new File(variables.get("pluginslocalgeneralconf"));
			Result result = new StreamResult(file);
			TransformerFactory transformerFactory = TransformerFactory
					.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(
					"{http:xml.apache.org/xslt}indent-amount", "4");
			transformer.transform(source, result);
			c.cd(variables.get("remoteuserhome") + "/twister/config/");
			FileInputStream in = new FileInputStream(file);
			c.put(in, file.getName());
			in.close();
			System.out.println("Saved " + file.getName() + " to: "
					+ variables.get("remoteuserhome") + "/twister/config/");
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
}