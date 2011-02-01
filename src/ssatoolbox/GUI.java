/*

Copyright (c) 2010, Jan Saputra Müller, Paul von Bünau, Frank C. Meinecke,
Franz J. Kiraly and Klaus-Robert Müller.
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice, this
list of conditions and the following disclaimer in the documentation and/or other
 materials provided with the distribution.

* Neither the name of the Berlin Institute of Technology (Technische Universität
Berlin) nor the names of its contributors may be used to endorse or promote
products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 */


package ssatoolbox;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JScrollBar;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.filechooser.*;
import java.awt.Dimension;
import java.awt.FocusTraversalPolicy;
import java.awt.Point;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.util.LinkedList;
import javax.swing.ButtonGroup;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;
import javax.swing.*;

/**
 * Implementation of the GUI (standalone version of the toolbox).
 *
 * @author Jan Saputra Mueller, saputra@cs.tu-berlin.de
 */
public class GUI extends javax.swing.JFrame implements Logger {
    private static final String FILESEL_CSV_FILTER = "Comma-separated-values files (*.csv)";
    private static final String FILESEL_MAT_FILTER = "Matlab files (*.mat)";
    private static final String FILESEL_MATCSV_FILTER = "Matlab (*.mat) or CSV file (*.csv)";
    private static final String FILESEL_SAVEDIR_FILTER = "Directories only";

    public static final int STATE_NO_DATA = 0;
    public static final int STATE_SSA_RUNNING = 1;
    public static final int STATE_PARAMETRIZATION = 2;
    public static final int STATE_RESULT_AVAILABLE = 3;

    private Main controller = null;
    private int state = GUI.STATE_NO_DATA;

    private SSAParameters ssa_parameters = null;
    private Data data = null;

    private class MyFocusTraversalPolicy extends FocusTraversalPolicy {
        private LinkedList<Component> listComponents = new LinkedList<Component>();

        public MyFocusTraversalPolicy() {
            listComponents.add(tfNumberOfEpochs);
            listComponents.add(tfNumberOfStationarySources);
            listComponents.add(tfNumberOfRestarts);
            listComponents.add(cbMomentMean);
            listComponents.add(btStartStopSSA);            
        }

        @Override
        public Component getComponentAfter(Container aContainer, Component aComponent) {
            if(aContainer == panelControl) {
                for(int i = 0; i < listComponents.size(); i++) {
                    if(listComponents.get(i) == aComponent) {
                        if(i < listComponents.size()-1)
                                return listComponents.get(i+1);
                        else
                                return listComponents.getFirst();
                    }
                }

                            throw new RuntimeException("Component not found!");



            } else {
                throw new RuntimeException("Not responsible for this container!");
            }
        }

        @Override
        public Component getComponentBefore(Container aContainer, Component aComponent) {
           if(aContainer == panelControl) {
            for(int i = 0; i < listComponents.size(); i++) {
                if(listComponents.get(i) == aComponent) {
                    if(i >= 1)
                            return listComponents.get(i-1);
                    else
                            return listComponents.getLast();
                }
            }

            throw new RuntimeException("Component not found!");
           } else {
            throw new RuntimeException("Not responsible for this container!");
           }

        }

        @Override
        public Component getFirstComponent(Container aContainer) {
            return listComponents.getFirst();
        }

        @Override
        public Component getLastComponent(Container aContainer) {
            return listComponents.getLast();
        }

        @Override
        public Component getDefaultComponent(Container aContainer) {
            return listComponents.getFirst();
        }
    }


    /** Creates new form MainFrame */
    public GUI(Main c, SSAParameters p, Data d) {
        controller = c;
        ssa_parameters = p;
        data = d;
        
        initComponents();

//        buildLookAndFeelMenu();

        final JScrollBar vsb = scrpLog.getVerticalScrollBar();
        vsb.addAdjustmentListener(new AdjustmentListener() {
           public void adjustmentValueChanged(AdjustmentEvent arg0) {
              if(!arg0.getValueIsAdjusting())
                  vsb.setValue(vsb.getMaximum());
           }
        }); 

        data.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent arg0) {
               setValuesFromModel();
            }
        });

        ssa_parameters.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent arg0) {
              setValuesFromModel();
            }
        });

        panelControl.setFocusTraversalPolicy(new MyFocusTraversalPolicy());

        setTitle("SSA Toolbox " + controller.SSA_VERSION);

        setValuesFromModel();

    }

    private void setEnableArrayWise(JComponent [] comps, boolean enabled) {
        for(int i = 0; i < comps.length; i++) comps[i].setEnabled(enabled);
    }

    private void setVisibleArrayWise(JComponent [] comps, boolean visible) {
        for(int i = 0; i < comps.length; i++) comps[i].setVisible(visible);
    }


    /**
     * Sets the state of the GUI. There are four different states:
     * STATE_NO_DATA, STATE_PARAMETRIZATION, STATE_RESULT_AVAILABLE, STATE_SSA_RUNNING.
     */
    public void setGUIState(int s) {
        state = s;

        final JComponent parameterComponents [] = new JComponent[]
            { rbEpochEquallySized, rbUseCustomEpochDefinition, tfNumberOfEpochs,
                tfNumberOfRestarts, tfNumberOfStationarySources, cbMomentMean,
                lbParameters, lbNumberOfRestarts, lbNumberOfStationarySources, btLoadCustomEpochDef,
                cbMomentCovMat, cbMomentMean, miLoadEpochDefinitionCSV, lbMoments, lbCustomEpochDefInfo
        };

        final JComponent loadDataMenuItems [] = new JComponent[]
            { miLoadTimeseries };

        final JComponent saveDataMenuItems [] = new JComponent[]
            {
                miSaveNonStationaryBasisCSV, miSaveNonstationaryProjectionCSV,
                miSaveNonstationarySignalsCSV, miSaveResultsMatlab, miSaveResultsCSV, miSaveStationaryBasisCSV,
                miSaveStationaryProjectionCSV, miSaveStationarySignalsCSV, lbCaptionResults,
                lbCaptionOutputDataformat, rbChannelsXTime, rbTimeXChannels, combSave, btSaveResults
            };

        final JComponent dataInfoComponents [] = new JComponent[]
            {
                lbCaptionResults, lbCaptionNumberOfDims, lbCaptionNumberOfSamples,
                lbFilename, lbNumberOfDims, lbNumberOfSamples,
                lbCaptionFilename, lbCaptionInputDataformat, lbCaptionOutputDataformat,
                lbInputDataformat, rbChannelsXTime, rbTimeXChannels
            };


        switch(state) {
            case GUI.STATE_NO_DATA:
                setEnableArrayWise(parameterComponents, false);
                setEnableArrayWise(loadDataMenuItems, true);
                setEnableArrayWise(saveDataMenuItems, false);
                btStartStopSSA.setEnabled(false);
                btStartStopSSA.setText("Start SSA");
                break;

            case GUI.STATE_PARAMETRIZATION:
                setEnableArrayWise(parameterComponents, true);
                setEnableArrayWise(loadDataMenuItems, true);
                setEnableArrayWise(saveDataMenuItems, false);
                rbUseCustomEpochDefinition.setEnabled(data.hasCustomEpochDefinition());            
                btStartStopSSA.setEnabled(true);
                btStartStopSSA.setText("Start SSA");
                break;

            case GUI.STATE_RESULT_AVAILABLE:
                setEnableArrayWise(parameterComponents, true);
                setEnableArrayWise(loadDataMenuItems, true);
                setEnableArrayWise(saveDataMenuItems, true);
                rbUseCustomEpochDefinition.setEnabled(data.hasCustomEpochDefinition());    
                btStartStopSSA.setEnabled(true);
                btStartStopSSA.setText("Start SSA");
                break;
                           
            case GUI.STATE_SSA_RUNNING:
                setEnableArrayWise(parameterComponents, false);
                setEnableArrayWise(loadDataMenuItems, false);
                setEnableArrayWise(saveDataMenuItems, false);                
                btStartStopSSA.setEnabled(true);
                btStartStopSSA.setText("Stop SSA");
                break;
        }
    }

    private static final int FILESEL_MODE_OPEN = 1;
    private static final int FILESEL_MODE_SAVE = 2;
    private static final int FILESEL_MODE_SAVE_DIR = 3;


    private File chooseFile(String [] extension, String desc, int mode) {
        final String [] f_extension = extension;
        final String f_desc = desc;
        final int f_mode = mode;

        String lastDir = controller.toolboxConfig.getProperty("last_directory");
        JFileChooser chooser = new JFileChooser(lastDir);
        chooser.setFileFilter(new FileFilter() {
            public String getDescription() {
                return f_desc;
            }

            public boolean accept(File f) {
                if(f_mode == FILESEL_MODE_SAVE_DIR) {
                    return f.isDirectory();
                } else {
                    for(int i = 0; i < f_extension.length; i++)
                        if(f.getName().toLowerCase().endsWith(f_extension[i]) || f.isDirectory()) return true;
                    return false;
                }
                
            }
        });

        if(mode == FILESEL_MODE_SAVE_DIR)
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        else
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        int rc = 0;
        switch(mode) {
            case FILESEL_MODE_OPEN:
                rc = chooser.showOpenDialog(this);
                break;

            case FILESEL_MODE_SAVE_DIR:
            case FILESEL_MODE_SAVE:
                rc = chooser.showSaveDialog(this);
                break;


            default: throw new RuntimeException("Illegal file selection mode");
        }

        if(rc == JFileChooser.APPROVE_OPTION)
        {
            controller.toolboxConfig.setProperty("last_directory", chooser.getSelectedFile().getParent());
            controller.toolboxConfig.saveProperties();
            return chooser.getSelectedFile();
        }
        else
            return null;
    }

    public void appendToLog(String str) {
        taLog.append(str + "\n");
    }
   
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        bgrpEpochizationMethod = new javax.swing.ButtonGroup();
        dialogCitation = new javax.swing.JDialog();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jLabel2 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTextArea2 = new javax.swing.JTextArea();
        jButton1 = new javax.swing.JButton();
        jTextField1 = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        jTextField2 = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        bgrpOutputDataformat = new javax.swing.ButtonGroup();
        dialogAbout = new javax.swing.JDialog();
        jPanel1 = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        jButton2 = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        panelContents = new javax.swing.JPanel();
        panelControl = new javax.swing.JPanel();
        btStartStopSSA = new javax.swing.JButton();
        panelData = new javax.swing.JPanel();
        lbNumberOfSamples = new javax.swing.JLabel();
        lbNumberOfDims = new javax.swing.JLabel();
        lbCaptionNumberOfDims = new javax.swing.JLabel();
        lbCaptionNumberOfSamples = new javax.swing.JLabel();
        lbCaptionData = new javax.swing.JLabel();
        lbCaptionFilename = new javax.swing.JLabel();
        lbFilename = new javax.swing.JLabel();
        lbCaptionInputDataformat = new javax.swing.JLabel();
        lbInputDataformat = new javax.swing.JLabel();
        btLoadTimeseries = new javax.swing.JButton();
        panelParameters = new javax.swing.JPanel();
        lbParameters = new javax.swing.JLabel();
        lbNumberOfRestarts = new javax.swing.JLabel();
        rbEpochEquallySized = new javax.swing.JRadioButton();
        rbUseCustomEpochDefinition = new javax.swing.JRadioButton();
        tfNumberOfEpochs = new javax.swing.JTextField();
        btLoadCustomEpochDef = new javax.swing.JButton();
        tfNumberOfStationarySources = new javax.swing.JTextField();
        lbNumberOfStationarySources = new javax.swing.JLabel();
        tfNumberOfRestarts = new javax.swing.JTextField();
        panelSSAMoments = new javax.swing.JPanel();
        lbMoments = new javax.swing.JLabel();
        cbMomentCovMat = new javax.swing.JCheckBox();
        cbMomentMean = new javax.swing.JCheckBox();
        lbCustomEpochDefInfo = new javax.swing.JLabel();
        panelResults = new javax.swing.JPanel();
        lbCaptionResults = new javax.swing.JLabel();
        btSaveResults = new javax.swing.JButton();
        combSave = new javax.swing.JComboBox();
        panelOutputFormat = new javax.swing.JPanel();
        lbCaptionOutputDataformat = new javax.swing.JLabel();
        rbChannelsXTime = new javax.swing.JRadioButton();
        rbTimeXChannels = new javax.swing.JRadioButton();
        panelLog = new javax.swing.JPanel();
        scrpLog = new javax.swing.JScrollPane();
        taLog = new javax.swing.JTextArea();
        jMenuBar2 = new javax.swing.JMenuBar();
        jMenu3 = new javax.swing.JMenu();
        miLoadTimeseries = new javax.swing.JMenuItem();
        miLoadEpochDefinitionCSV = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JSeparator();
        miSaveResultsMatlab = new javax.swing.JMenuItem();
        miSaveResultsCSV = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JSeparator();
        miSaveStationaryBasisCSV = new javax.swing.JMenuItem();
        miSaveNonStationaryBasisCSV = new javax.swing.JMenuItem();
        miSaveStationaryProjectionCSV = new javax.swing.JMenuItem();
        miSaveNonstationaryProjectionCSV = new javax.swing.JMenuItem();
        miSaveStationarySignalsCSV = new javax.swing.JMenuItem();
        miSaveNonstationarySignalsCSV = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JSeparator();
        miExit = new javax.swing.JMenuItem();
        jMenu4 = new javax.swing.JMenu();
        jMenuItem13 = new javax.swing.JMenuItem();
        jMenuItem14 = new javax.swing.JMenuItem();

        dialogCitation.getContentPane().setLayout(new java.awt.GridBagLayout());

        jPanel2.setLayout(new java.awt.GridBagLayout());

        jLabel1.setText("Please cite the following paper for the SSA method.");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
        jPanel2.add(jLabel1, gridBagConstraints);

        jTextArea1.setColumns(20);
        jTextArea1.setEditable(false);
        jTextArea1.setFont(new java.awt.Font("Courier New", 0, 13));
        jTextArea1.setRows(5);
        jTextArea1.setText("@Article{PRL:SSA:2009,\n  title = {Finding Stationary Subspaces in Multivariate Time Series},\n  author = {von B\\\"unau, Paul  and Meinecke, Frank C. and Kir\\'aly, Franz C. and M\\\"uller, Klaus-Robert },\n  journal = {Phys. Rev. Lett.},\n  volume = {103},\n  number = {21},\n  pages = {214101},\n  numpages = {4},\n  year = {2009},\n  month = {Nov},\n  doi = {10.1103/PhysRevLett.103.214101},\n  publisher = {American Physical Society}\n}\n");
        jScrollPane2.setViewportView(jTextArea1);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel2.add(jScrollPane2, gridBagConstraints);

	/*
        jLabel2.setText("Please cite the following paper for the SSA Toolbox.");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 30;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 0);
        jPanel2.add(jLabel2, gridBagConstraints);

        jTextArea2.setColumns(20);
        jTextArea2.setFont(new java.awt.Font("Courier New", 0, 13));
        jTextArea2.setRows(5);
        jTextArea2.setText("@Article{JMLR:SSAToolbox:2009,\n  author =   {Jan Saputra M{\\\"uller} and\n               Paul von B{\\\"u}nau and\n               Frank C.~Meinecke and\n               Franz J.~Kir\\'{a}ly and \n               Klaus-Robert M{\\\"u}ller},\n  title =  {SSA Toolbox}, \n  journal =   {Journal of Machine Learning Research (accepted)},\n  year =   2009\n}");
        jScrollPane3.setViewportView(jTextArea2);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 40;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel2.add(jScrollPane3, gridBagConstraints);
		*/

        jButton1.setText("Close");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 50;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
        jPanel2.add(jButton1, gridBagConstraints);

        jTextField1.setEditable(false);
        jTextField1.setText("http://www.jmlr.org/LINK");
        jTextField1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField1ActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 31;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        //jPanel2.add(jTextField1, gridBagConstraints);

        jLabel3.setText("Reference in BibTeX format:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 32;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 0);
        //jPanel2.add(jLabel3, gridBagConstraints);

        jTextField2.setEditable(false);
        jTextField2.setText("http://dx.doi.org/10.1103/PhysRevLett.103.214101");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 0);
        jPanel2.add(jTextField2, gridBagConstraints);

        jLabel4.setText("Reference in BibTeX format:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        //jPanel2.add(jLabel4, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        dialogCitation.getContentPane().add(jPanel2, gridBagConstraints);

        dialogAbout.setTitle("About the SSA Toolbox");

        jPanel1.setLayout(new java.awt.GridBagLayout());

        jLabel5.setFont(new java.awt.Font("DejaVu Sans", 1, 13)); // NOI18N
        jLabel5.setText("SSA Toolbox " + controller.SSA_VERSION);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 0);
        jPanel1.add(jLabel5, gridBagConstraints);

        jButton2.setText("Close");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 20;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel1.add(jButton2, gridBagConstraints);

        jLabel6.setText("Copyright (c) 2010 Jan Saputra Mueller, Paul von Buenau, ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        jPanel1.add(jLabel6, gridBagConstraints);

        jLabel7.setText("Licensed under the BSD license (see file COPYING for details)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        jPanel1.add(jLabel7, gridBagConstraints);

        jLabel8.setText("www.stationary-subspace-analysis.org");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        jPanel1.add(jLabel8, gridBagConstraints);

        jLabel9.setText("Frank C. Meinecke, Franz J. Kiraly and Klaus-Robert Mueller");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        jPanel1.add(jLabel9, gridBagConstraints);

        jLabel10.setText("All rights reserved\n");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        jPanel1.add(jLabel10, gridBagConstraints);

        dialogAbout.getContentPane().add(jPanel1, java.awt.BorderLayout.CENTER);

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("SSA");
        setMinimumSize(new java.awt.Dimension(800, 800));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        panelContents.setMinimumSize(new java.awt.Dimension(300, 380));
        panelContents.setPreferredSize(new java.awt.Dimension(300, 380));
        panelContents.setLayout(new java.awt.GridBagLayout());

        panelControl.setFocusTraversalPolicyProvider(true);
        panelControl.setMinimumSize(new java.awt.Dimension(200, 200));
        panelControl.setPreferredSize(new java.awt.Dimension(690, 250));
        panelControl.setLayout(new java.awt.GridBagLayout());

        btStartStopSSA.setFont(new java.awt.Font("DejaVu Sans", 1, 13));
        btStartStopSSA.setText("Run SSA");
        btStartStopSSA.setMinimumSize(new java.awt.Dimension(150, 27));
        btStartStopSSA.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btStartStopSSAActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 15);
        panelControl.add(btStartStopSSA, gridBagConstraints);

        panelData.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        panelData.setLayout(new java.awt.GridBagLayout());

        lbNumberOfSamples.setText("n/a");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 10, 0, 0);
        panelData.add(lbNumberOfSamples, gridBagConstraints);

        lbNumberOfDims.setText("n/a");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        panelData.add(lbNumberOfDims, gridBagConstraints);

        lbCaptionNumberOfDims.setText("Number of channels:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        panelData.add(lbCaptionNumberOfDims, gridBagConstraints);

        lbCaptionNumberOfSamples.setText("Total number of samples:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 10, 0, 0);
        panelData.add(lbCaptionNumberOfSamples, gridBagConstraints);

        lbCaptionData.setFont(new java.awt.Font("DejaVu Sans", 1, 13));
        lbCaptionData.setText("Data");
        lbCaptionData.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        lbCaptionData.setVerticalTextPosition(javax.swing.SwingConstants.TOP);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 10, 0);
        panelData.add(lbCaptionData, gridBagConstraints);

        lbCaptionFilename.setText("Timeseries file:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        panelData.add(lbCaptionFilename, gridBagConstraints);

        lbFilename.setText("n/a");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        panelData.add(lbFilename, gridBagConstraints);

        lbCaptionInputDataformat.setText("Input data format:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        panelData.add(lbCaptionInputDataformat, gridBagConstraints);

        lbInputDataformat.setText("n/a");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 10, 0, 0);
        panelData.add(lbInputDataformat, gridBagConstraints);

        btLoadTimeseries.setText("Load timeseries");
        btLoadTimeseries.setMinimumSize(new java.awt.Dimension(150, 27));
        btLoadTimeseries.setPreferredSize(new java.awt.Dimension(150, 27));
        btLoadTimeseries.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btLoadTimeseriesActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHEAST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 10);
        panelData.add(btLoadTimeseries, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 20;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 5);
        panelControl.add(panelData, gridBagConstraints);

        panelParameters.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        panelParameters.setLayout(new java.awt.GridBagLayout());

        lbParameters.setFont(new java.awt.Font("DejaVu Sans", 1, 13));
        lbParameters.setText("Parameters");
        lbParameters.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        lbParameters.setFocusable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 10, 0);
        panelParameters.add(lbParameters, gridBagConstraints);

        lbNumberOfRestarts.setText("Number of restarts:");
        lbNumberOfRestarts.setFocusable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        panelParameters.add(lbNumberOfRestarts, gridBagConstraints);

        bgrpEpochizationMethod.add(rbEpochEquallySized);
        rbEpochEquallySized.setSelected(true);
        rbEpochEquallySized.setText("Epochs: equally sized. Number:");
        rbEpochEquallySized.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                rbEpochEquallySizedStateChanged(evt);
            }
        });
        rbEpochEquallySized.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rbEpochEquallySizedActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        panelParameters.add(rbEpochEquallySized, gridBagConstraints);

        bgrpEpochizationMethod.add(rbUseCustomEpochDefinition);
        rbUseCustomEpochDefinition.setText("Epochs: according to custom definition.");
        rbUseCustomEpochDefinition.setEnabled(false);
        rbUseCustomEpochDefinition.setFocusable(false);
        rbUseCustomEpochDefinition.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                rbUseCustomEpochDefinitionStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 10);
        panelParameters.add(rbUseCustomEpochDefinition, gridBagConstraints);

        tfNumberOfEpochs.setColumns(5);
        tfNumberOfEpochs.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        tfNumberOfEpochs.setMaximumSize(new java.awt.Dimension(10, 25));
        tfNumberOfEpochs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tfNumberOfEpochsActionPerformed(evt);
            }
        });
        tfNumberOfEpochs.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                tfNumberOfEpochsFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                tfNumberOfEpochsFocusLost(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panelParameters.add(tfNumberOfEpochs, gridBagConstraints);

        btLoadCustomEpochDef.setText("Load custom definition");
        btLoadCustomEpochDef.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btLoadCustomEpochDefActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 15, 0, 0);
        panelParameters.add(btLoadCustomEpochDef, gridBagConstraints);

        tfNumberOfStationarySources.setColumns(5);
        tfNumberOfStationarySources.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        tfNumberOfStationarySources.setMaximumSize(new java.awt.Dimension(10, 25));
        tfNumberOfStationarySources.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tfNumberOfStationarySourcesActionPerformed(evt);
            }
        });
        tfNumberOfStationarySources.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                tfNumberOfStationarySourcesFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                tfNumberOfStationarySourcesFocusLost(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 10);
        panelParameters.add(tfNumberOfStationarySources, gridBagConstraints);

        lbNumberOfStationarySources.setText("Number of stationary sources:");
        lbNumberOfStationarySources.setFocusable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 10);
        panelParameters.add(lbNumberOfStationarySources, gridBagConstraints);

        tfNumberOfRestarts.setColumns(5);
        tfNumberOfRestarts.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        tfNumberOfRestarts.setMaximumSize(new java.awt.Dimension(10, 50));
        tfNumberOfRestarts.setMinimumSize(new java.awt.Dimension(10, 50));
        tfNumberOfRestarts.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                tfNumberOfRestartsFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                tfNumberOfRestartsFocusLost(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 10);
        panelParameters.add(tfNumberOfRestarts, gridBagConstraints);

        panelSSAMoments.setLayout(new java.awt.GridBagLayout());

        lbMoments.setText("Run SSA with respect to");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 10);
        panelSSAMoments.add(lbMoments, gridBagConstraints);

        cbMomentCovMat.setSelected(true);
        cbMomentCovMat.setText("Covariance matrix");
        cbMomentCovMat.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbMomentCovMatActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
        panelSSAMoments.add(cbMomentCovMat, gridBagConstraints);

        cbMomentMean.setSelected(true);
        cbMomentMean.setText("Mean");
        cbMomentMean.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                cbMomentMeanStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
        panelSSAMoments.add(cbMomentMean, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        panelParameters.add(panelSSAMoments, gridBagConstraints);

        lbCustomEpochDefInfo.setText("jLabel5");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 15, 0, 0);
        panelParameters.add(lbCustomEpochDefInfo, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 100;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panelControl.add(panelParameters, gridBagConstraints);

        panelResults.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        panelResults.setLayout(new java.awt.GridBagLayout());

        lbCaptionResults.setFont(new java.awt.Font("DejaVu Sans", 1, 13));
        lbCaptionResults.setText("Results");
        lbCaptionResults.setMaximumSize(new java.awt.Dimension(54, 20));
        lbCaptionResults.setMinimumSize(new java.awt.Dimension(54, 20));
        lbCaptionResults.setVerticalTextPosition(javax.swing.SwingConstants.TOP);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 10, 0);
        panelResults.add(lbCaptionResults, gridBagConstraints);

        btSaveResults.setText("Save results");
        btSaveResults.setMinimumSize(new java.awt.Dimension(150, 27));
        btSaveResults.setPreferredSize(new java.awt.Dimension(150, 27));
        btSaveResults.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btSaveResultsActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHEAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 10);
        panelResults.add(btSaveResults, gridBagConstraints);

        combSave.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Save: all results (Matlab)", "Save: all results (CSV)", "Save: stationary sources (CSV)", "Save: non-stationary sources (CSV)", "Save: stationary basis (CSV)", "Save: non-stationary basis (CSV)", "Save: filter for stationary sources (CSV)", "Save: filter for non-stationary sources (CSV)" }));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHEAST;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 10);
        panelResults.add(combSave, gridBagConstraints);

        panelOutputFormat.setLayout(new java.awt.GridBagLayout());

        lbCaptionOutputDataformat.setText("Output data format:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 10);
        panelOutputFormat.add(lbCaptionOutputDataformat, gridBagConstraints);

        bgrpOutputDataformat.add(rbChannelsXTime);
        rbChannelsXTime.setText("Channels x Time");
        rbChannelsXTime.setFocusable(false);
        rbChannelsXTime.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rbChannelsXTimeActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panelOutputFormat.add(rbChannelsXTime, gridBagConstraints);

        bgrpOutputDataformat.add(rbTimeXChannels);
        rbTimeXChannels.setText("Time x Channels");
        rbTimeXChannels.setFocusable(false);
        rbTimeXChannels.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                rbTimeXChannelsStateChanged(evt);
            }
        });
        rbTimeXChannels.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rbTimeXChannelsActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        panelOutputFormat.add(rbTimeXChannels, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        panelResults.add(panelOutputFormat, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 300;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panelControl.add(panelResults, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipady = 200;
        gridBagConstraints.weightx = 1.0;
        panelContents.add(panelControl, gridBagConstraints);

        panelLog.setLayout(new java.awt.BorderLayout());

        taLog.setColumns(20);
        taLog.setRows(5);
        scrpLog.setViewportView(taLog);

        panelLog.add(scrpLog, java.awt.BorderLayout.CENTER);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.2;
        panelContents.add(panelLog, gridBagConstraints);

        getContentPane().add(panelContents, java.awt.BorderLayout.CENTER);

        jMenu3.setText("File");

        miLoadTimeseries.setText("Load timeseries (Matlab or CSV)");
        miLoadTimeseries.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miLoadTimeseriesActionPerformed(evt);
            }
        });
        jMenu3.add(miLoadTimeseries);

        miLoadEpochDefinitionCSV.setText("Load epoch definition (CSV)");
        miLoadEpochDefinitionCSV.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miLoadEpochDefinitionCSVActionPerformed(evt);
            }
        });
        jMenu3.add(miLoadEpochDefinitionCSV);
        jMenu3.add(jSeparator1);

        miSaveResultsMatlab.setText("Save all results (Matlab)");
        miSaveResultsMatlab.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miSaveResultsMatlabActionPerformed(evt);
            }
        });
        jMenu3.add(miSaveResultsMatlab);

        miSaveResultsCSV.setText("Save all results (CSV)");
        miSaveResultsCSV.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miSaveResultsCSVActionPerformed(evt);
            }
        });
        jMenu3.add(miSaveResultsCSV);
        jMenu3.add(jSeparator3);

        miSaveStationaryBasisCSV.setText("Save stationary basis (CSV)");
        miSaveStationaryBasisCSV.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miSaveStationaryBasisCSVActionPerformed(evt);
            }
        });
        jMenu3.add(miSaveStationaryBasisCSV);

        miSaveNonStationaryBasisCSV.setText("Save non-stationary basis (CSV)");
        miSaveNonStationaryBasisCSV.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miSaveNonStationaryBasisCSVActionPerformed(evt);
            }
        });
        jMenu3.add(miSaveNonStationaryBasisCSV);

        miSaveStationaryProjectionCSV.setText("Save stationary projection (CSV)");
        miSaveStationaryProjectionCSV.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miSaveStationaryProjectionCSVActionPerformed(evt);
            }
        });
        jMenu3.add(miSaveStationaryProjectionCSV);

        miSaveNonstationaryProjectionCSV.setText("Save non-stationary projection (CSV)");
        miSaveNonstationaryProjectionCSV.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miSaveNonstationaryProjectionCSVActionPerformed(evt);
            }
        });
        jMenu3.add(miSaveNonstationaryProjectionCSV);

        miSaveStationarySignalsCSV.setText("Save stationary signals (CSV)");
        miSaveStationarySignalsCSV.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miSaveStationarySignalsCSVActionPerformed(evt);
            }
        });
        jMenu3.add(miSaveStationarySignalsCSV);

        miSaveNonstationarySignalsCSV.setText("Save non-stationary signals (CSV)");
        miSaveNonstationarySignalsCSV.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miSaveNonstationarySignalsCSVActionPerformed(evt);
            }
        });
        jMenu3.add(miSaveNonstationarySignalsCSV);
        jMenu3.add(jSeparator2);

        miExit.setText("Exit");
        miExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miExitActionPerformed(evt);
            }
        });
        jMenu3.add(miExit);

        jMenuBar2.add(jMenu3);

				// Settings menu
				JMenu mnuMatrixLibrary = new JMenu("Matrix Library");
				ButtonGroup bgrpMatLib = new ButtonGroup();
				JRadioButtonMenuItem rbmiCOLT = new JRadioButtonMenuItem("COLT");
				rbmiCOLT.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                appendToLog("Switching to COLT library...");
            	SSAMatrix.setGlobalLib(SSAMatrix.COLT);
                controller.toolboxConfig.setProperty("matrix_library", "colt");
                controller.toolboxConfig.saveProperties();
            }
        });

				JRadioButtonMenuItem rbmiJBLAS = new JRadioButtonMenuItem("jBLAS");
				rbmiJBLAS.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                appendToLog("Switching to jBLAS library...");
            	SSAMatrix.setGlobalLib(SSAMatrix.JBLAS);
                controller.toolboxConfig.setProperty("matrix_library", "jblas");
                controller.toolboxConfig.saveProperties();
            }
        });

				bgrpMatLib.add(rbmiCOLT);
				bgrpMatLib.add(rbmiJBLAS);

                                String lib = controller.toolboxConfig.getProperty("matrix_library");
                                if(lib == null || lib.equals("colt"))
                                {
				    rbmiCOLT.setSelected(true);
                                    SSAMatrix.setGlobalLib(SSAMatrix.COLT);
                                }
                                else if(lib.equals("jblas"))
                                {
				    rbmiJBLAS.setSelected(true);
                                    SSAMatrix.setGlobalLib(SSAMatrix.JBLAS);
                                }
                                else
                                {
                                    appendToLog("Error: Unknown library \"" + lib + "\".");
                                    appendToLog("Switching to COLT library...");
				    rbmiCOLT.setSelected(true);
                                    SSAMatrix.setGlobalLib(SSAMatrix.COLT);
                                }

				mnuMatrixLibrary.add(rbmiCOLT);
				mnuMatrixLibrary.add(rbmiJBLAS);

				JMenu mnuSettings = new JMenu("Settings");
				mnuSettings.add(mnuMatrixLibrary);
				jMenuBar2.add(mnuSettings);


				// ------------------------------------					

        jMenu4.setText("Help");

        jMenuItem13.setText("Citation");
        jMenuItem13.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem13ActionPerformed(evt);
            }
        });

        jMenu4.add(jMenuItem13);

        jMenuItem14.setText("About");
        jMenuItem14.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem14ActionPerformed(evt);
            }
        });
        jMenu4.add(jMenuItem14);

        jMenuBar2.add(jMenu4);

        setJMenuBar(jMenuBar2);

        java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        setBounds((screenSize.width-780)/2, (screenSize.height-668)/2, 780, 668);
    }// </editor-fold>//GEN-END:initComponents

    private void rbEpochEquallySizedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rbEpochEquallySizedActionPerformed
        // TODO add your handling code here:
}//GEN-LAST:event_rbEpochEquallySizedActionPerformed

    private void btStartStopSSAActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btStartStopSSAActionPerformed
        if(state == GUI.STATE_SSA_RUNNING)
            controller.stopSSA();
        else
            controller.runSSA();
}//GEN-LAST:event_btStartStopSSAActionPerformed

    private void jMenuItem14ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem14ActionPerformed
        Point screenpos = this.getLocationOnScreen();

        dialogAbout.pack();
        dialogAbout.setSize(450, 180);
        dialogAbout.setLocation(screenpos.x + 50, screenpos.y + 50);
        dialogAbout.setVisible(true);

    }//GEN-LAST:event_jMenuItem14ActionPerformed

    private void miExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miExitActionPerformed
        controller.exit();
}//GEN-LAST:event_miExitActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        controller.exit();
    }//GEN-LAST:event_formWindowClosing

    private void tfNumberOfEpochsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tfNumberOfEpochsActionPerformed
    }//GEN-LAST:event_tfNumberOfEpochsActionPerformed

    private void setValuesFromModel() {
        if( state == GUI.STATE_PARAMETRIZATION ||
            state == GUI.STATE_RESULT_AVAILABLE )  {
            rbUseCustomEpochDefinition.setEnabled(data.hasCustomEpochDefinition());
        }

        rbUseCustomEpochDefinition.setSelected(data.useCustomEpochDefinition());

        tfNumberOfEpochs.setText(numberToText(data.getNumberOfEqualSizeEpochs()));
        tfNumberOfStationarySources.setText(numberToText(ssa_parameters.getNumberOfStationarySources()));
        tfNumberOfRestarts.setText(numberToText(ssa_parameters.getNumberOfRestarts()));
        //cbMomentMean.setSelected(ssa_parameters.isIgnoreChangeInMeans());

        if( data.getTimeseriesFile() != null ) {
            lbFilename.setText(data.getTimeseriesFile().getName());
            lbNumberOfDims.setText(data.getNumberOfDimensions() + "");
            lbNumberOfSamples.setText(data.getTotalNumberOfSamples() + "");
        }

        if(data.hasCustomEpochDefinition()) {
            lbCustomEpochDefInfo.setText("Epoch file: " + data.getEpochDefinitionFile().getName());
        } else {
            lbCustomEpochDefInfo.setText("No custom epoch definition available");
        }

        switch(data.getInputDataformat()) {
            case Data.DATAFORMAT_CHANNELS_X_TIME:
                lbInputDataformat.setText("Channels x Time");
                break;

            case Data.DATAFORMAT_TIME_X_CHANNELS:
                lbInputDataformat.setText("Time x Channels");
                break;
        }

        switch(data.getOutputDataformat()) {
            case Data.DATAFORMAT_CHANNELS_X_TIME:
                rbChannelsXTime.setSelected(true);
                break;

            case Data.DATAFORMAT_TIME_X_CHANNELS:
                rbTimeXChannels.setSelected(true);
                break;
        }
    }

    private String numberToText(int n) {
        if(n == -1)
            return "";
        else
            return n + "";
    }

    private void tfNumberOfEpochsFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_tfNumberOfEpochsFocusLost
        tfNumberOfEpochs.setSelectionStart(0);
        tfNumberOfEpochs.setSelectionEnd(0);

        try {
            final int n = Integer.parseInt(tfNumberOfEpochs.getText());

            try
            {
                data.setNumberOfEqualSizeEpochs(n);
            }
            catch(IllegalArgumentException e)
            {
                appendToLog(e.getMessage());
                setValuesFromModel();
            }

        } catch(RuntimeException ex) {
            setValuesFromModel();
        }
    }//GEN-LAST:event_tfNumberOfEpochsFocusLost

    private void tfNumberOfStationarySourcesFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_tfNumberOfStationarySourcesFocusLost
        tfNumberOfStationarySources.setSelectionStart(0);
        tfNumberOfStationarySources.setSelectionEnd(0);

        try {
            final int n = Integer.parseInt(tfNumberOfStationarySources.getText());

            if(n > 0 )
                ssa_parameters.setNumberOfStationarySources(n);
            else
                setValuesFromModel();
        } catch(RuntimeException ex) {
            setValuesFromModel();
        }
    }//GEN-LAST:event_tfNumberOfStationarySourcesFocusLost

    private void tfNumberOfRestartsFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_tfNumberOfRestartsFocusLost
        tfNumberOfRestarts.setSelectionStart(0);
        tfNumberOfRestarts.setSelectionEnd(0);
        
        try {
            ssa_parameters.setNumberOfRestarts(Integer.parseInt(tfNumberOfRestarts.getText()));
        } catch(RuntimeException ex) {
            setValuesFromModel();
        }
    }//GEN-LAST:event_tfNumberOfRestartsFocusLost

    private void rbEpochEquallySizedStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_rbEpochEquallySizedStateChanged
    }//GEN-LAST:event_rbEpochEquallySizedStateChanged

    private void rbUseCustomEpochDefinitionStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_rbUseCustomEpochDefinitionStateChanged
        data.setUseCustomEpochDefinition(rbUseCustomEpochDefinition.isSelected());
    }//GEN-LAST:event_rbUseCustomEpochDefinitionStateChanged

    private void cbMomentMeanStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_cbMomentMeanStateChanged
        ssa_parameters.setUseMean(cbMomentMean.isSelected());
    }//GEN-LAST:event_cbMomentMeanStateChanged

    private void miLoadTimeseriesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miLoadTimeseriesActionPerformed
        File f = chooseFile(new String [] { "csv", "mat" }, FILESEL_MATCSV_FILTER, FILESEL_MODE_OPEN);
        if(f != null) controller.loadTimeseries(f);
    }//GEN-LAST:event_miLoadTimeseriesActionPerformed

    private void miLoadEpochDefinitionCSVActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miLoadEpochDefinitionCSVActionPerformed
        File f = chooseFile(new String[] { "csv" }, FILESEL_CSV_FILTER, FILESEL_MODE_OPEN);
        if (f != null) controller.loadEpochDefinitionCSV(f);
    }//GEN-LAST:event_miLoadEpochDefinitionCSVActionPerformed

    private void miSaveStationaryBasisCSVActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miSaveStationaryBasisCSVActionPerformed
        saveStationaryBasisCSV();
    }//GEN-LAST:event_miSaveStationaryBasisCSVActionPerformed

    private void saveStationaryBasisCSV() {
        File f = chooseFile(new String [] {"csv"}, FILESEL_CSV_FILTER, FILESEL_MODE_SAVE);
        if (f!= null) controller.saveStationaryBasisCSV(f);
    }

    private void miSaveNonStationaryBasisCSVActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miSaveNonStationaryBasisCSVActionPerformed
        saveNonStationaryBasisCSV();
    }//GEN-LAST:event_miSaveNonStationaryBasisCSVActionPerformed

    private void saveNonStationaryBasisCSV() {
        File f = chooseFile(new String[] {"csv"}, FILESEL_CSV_FILTER, FILESEL_MODE_SAVE);
        if (f!= null) controller.saveNonstationaryBasisCSV(f);
    }

    private void miSaveStationaryProjectionCSVActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miSaveStationaryProjectionCSVActionPerformed
        saveStationaryProjectionsCSV();
    }//GEN-LAST:event_miSaveStationaryProjectionCSVActionPerformed

    private void saveStationaryProjectionsCSV() {
        File f = chooseFile(new String[] {"csv"}, FILESEL_CSV_FILTER, FILESEL_MODE_SAVE);
        if (f!= null) controller.saveStationaryProjectionCSV(f);
    }

    private void miSaveNonstationaryProjectionCSVActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miSaveNonstationaryProjectionCSVActionPerformed
        saveNonstationaryProjectionsCSV();
    }//GEN-LAST:event_miSaveNonstationaryProjectionCSVActionPerformed

    private void saveNonstationaryProjectionsCSV() {
        File f = chooseFile(new String[] {"csv"}, FILESEL_CSV_FILTER, FILESEL_MODE_SAVE);
        if (f!= null) controller.saveNonstationaryProjectionCSV(f);
    }

    private void miSaveStationarySignalsCSVActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miSaveStationarySignalsCSVActionPerformed
        saveStationarySignalsCSV();
    }//GEN-LAST:event_miSaveStationarySignalsCSVActionPerformed

    private void saveStationarySignalsCSV() {
        File f = chooseFile(new String [] {"csv"}, FILESEL_CSV_FILTER, FILESEL_MODE_SAVE);
        if (f!= null) controller.saveStationarySourcesCSV(f);
    }

    private void miSaveNonstationarySignalsCSVActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miSaveNonstationarySignalsCSVActionPerformed
        saveNonstationarySignalsCSV();
    }//GEN-LAST:event_miSaveNonstationarySignalsCSVActionPerformed

    private void saveNonstationarySignalsCSV() {
        File f = chooseFile(new String [] {"csv"}, FILESEL_CSV_FILTER, FILESEL_MODE_SAVE);
        if (f!= null) controller.saveNonstationarySourcesCSV(f);
    }

    private void miSaveResultsMatlabActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miSaveResultsMatlabActionPerformed
        saveAllResultsMatlab();
    }//GEN-LAST:event_miSaveResultsMatlabActionPerformed

    private void saveAllResultsMatlab() {
        File f = chooseFile(new String [] {"mat"}, FILESEL_MAT_FILTER, FILESEL_MODE_SAVE);
        if (f!= null) controller.saveResultMatlab(f);
    }

    private void saveAllResultsCSV() {
        File f = chooseFile(null, FILESEL_SAVEDIR_FILTER, FILESEL_MODE_SAVE_DIR);

        if(f != null) {
            // Save all results to chosen directory.

            final String absPath = f.getAbsolutePath() + File.separator;
            controller.saveStationarySourcesCSV(new File(absPath + "stationary_sources.csv"));
            controller.saveNonstationarySourcesCSV(new File(absPath + "nonstationary_sources.csv"));

            controller.saveStationaryBasisCSV(new File(absPath + "stationary_basis.csv"));
            controller.saveNonstationaryBasisCSV(new File(absPath + "nonstationary_basis.csv"));
            
            controller.saveStationaryProjectionCSV(new File(absPath + "stationary_projection.csv"));
            controller.saveNonstationaryProjectionCSV(new File(absPath + "nonstationary_projection.csv"));
        }
    }

    private void tfNumberOfEpochsFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_tfNumberOfEpochsFocusGained
        tfNumberOfEpochs.setSelectionStart(0);
        tfNumberOfEpochs.setSelectionEnd(tfNumberOfEpochs.getText().length());
    }//GEN-LAST:event_tfNumberOfEpochsFocusGained

    private void tfNumberOfStationarySourcesFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_tfNumberOfStationarySourcesFocusGained
        tfNumberOfStationarySources.setSelectionStart(0);
        tfNumberOfStationarySources.setSelectionEnd(tfNumberOfStationarySources.getText().length());
    }//GEN-LAST:event_tfNumberOfStationarySourcesFocusGained

    private void tfNumberOfRestartsFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_tfNumberOfRestartsFocusGained
        tfNumberOfRestarts.setSelectionStart(0);
        tfNumberOfRestarts.setSelectionEnd(tfNumberOfRestarts.getText().length());
    }//GEN-LAST:event_tfNumberOfRestartsFocusGained

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        dialogCitation.setVisible(false);
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jMenuItem13ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem13ActionPerformed
        Point screenpos = this.getLocationOnScreen();

        dialogCitation.pack();
        dialogCitation.setSize(400,500);
        dialogCitation.setLocation(screenpos.x + 50, screenpos.y + 50);
        dialogCitation.setVisible(true);
        
    }//GEN-LAST:event_jMenuItem13ActionPerformed

    private void jTextField1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField1ActionPerformed

    private void rbTimeXChannelsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rbTimeXChannelsActionPerformed
        if(rbChannelsXTime.isSelected()) {
            data.setOutputDataformat(Data.DATAFORMAT_CHANNELS_X_TIME);
        } else {
            data.setOutputDataformat(Data.DATAFORMAT_TIME_X_CHANNELS);
        }
}//GEN-LAST:event_rbTimeXChannelsActionPerformed

    private void rbTimeXChannelsStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_rbTimeXChannelsStateChanged
  
        if(rbTimeXChannels.isSelected()) {
            data.setOutputDataformat(Data.DATAFORMAT_TIME_X_CHANNELS);
        } else {
            data.setOutputDataformat(Data.DATAFORMAT_CHANNELS_X_TIME);
        }
    }//GEN-LAST:event_rbTimeXChannelsStateChanged

    private void btLoadCustomEpochDefActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btLoadCustomEpochDefActionPerformed
        File f = chooseFile(new String [] {"csv"}, FILESEL_CSV_FILTER, FILESEL_MODE_OPEN);
        if (f != null) controller.loadEpochDefinitionCSV(f);
    }//GEN-LAST:event_btLoadCustomEpochDefActionPerformed

    private void btLoadTimeseriesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btLoadTimeseriesActionPerformed
        File f = chooseFile(new String [] { "csv", "mat" }, FILESEL_MATCSV_FILTER, FILESEL_MODE_OPEN);
        if(f != null) controller.loadTimeseries(f);
    }//GEN-LAST:event_btLoadTimeseriesActionPerformed

    private void btSaveResultsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btSaveResultsActionPerformed
      
        switch(combSave.getSelectedIndex()) {
            case 0: // All results (Matlab)
                saveAllResultsMatlab();
                break;

            case 1: // All results (CSV)
                saveAllResultsCSV();
                break;

            case 2: // s-sources
                saveStationarySignalsCSV();
                break;

            case 3: // n-sources
                saveNonstationarySignalsCSV();
                break;

            case 4: // s-basis
                saveStationaryBasisCSV();
                break;

            case 5: // n-basis
                saveNonStationaryBasisCSV();
                break;

            case 6: // s-filter
                saveStationaryProjectionsCSV();
                break;

            case 7: // n-filter
                saveNonstationaryProjectionsCSV();
                break;
        }
    }//GEN-LAST:event_btSaveResultsActionPerformed

    private void miSaveResultsCSVActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miSaveResultsCSVActionPerformed
        saveAllResultsCSV();
    }//GEN-LAST:event_miSaveResultsCSVActionPerformed

    private void rbChannelsXTimeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rbChannelsXTimeActionPerformed
        if(rbChannelsXTime.isSelected()) {
            data.setOutputDataformat(Data.DATAFORMAT_CHANNELS_X_TIME);
        } else {
            data.setOutputDataformat(Data.DATAFORMAT_TIME_X_CHANNELS);
        }
    }//GEN-LAST:event_rbChannelsXTimeActionPerformed

    private void cbMomentCovMatActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbMomentCovMatActionPerformed
        ssa_parameters.setUseCovariance(cbMomentCovMat.isSelected());
    }//GEN-LAST:event_cbMomentCovMatActionPerformed

    private void tfNumberOfStationarySourcesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tfNumberOfStationarySourcesActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_tfNumberOfStationarySourcesActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        dialogAbout.setVisible(false);
    }//GEN-LAST:event_jButton2ActionPerformed
        

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup bgrpEpochizationMethod;
    private javax.swing.ButtonGroup bgrpOutputDataformat;
    private javax.swing.JButton btLoadCustomEpochDef;
    private javax.swing.JButton btLoadTimeseries;
    private javax.swing.JButton btSaveResults;
    private javax.swing.JButton btStartStopSSA;
    private javax.swing.JCheckBox cbMomentCovMat;
    private javax.swing.JCheckBox cbMomentMean;
    private javax.swing.JComboBox combSave;
    private javax.swing.JDialog dialogAbout;
    private javax.swing.JDialog dialogCitation;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenu jMenu4;
    private javax.swing.JMenuBar jMenuBar2;
    private javax.swing.JMenuItem jMenuItem13;
    private javax.swing.JMenuItem jMenuItem14;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextArea jTextArea2;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JLabel lbCaptionData;
    private javax.swing.JLabel lbCaptionFilename;
    private javax.swing.JLabel lbCaptionInputDataformat;
    private javax.swing.JLabel lbCaptionNumberOfDims;
    private javax.swing.JLabel lbCaptionNumberOfSamples;
    private javax.swing.JLabel lbCaptionOutputDataformat;
    private javax.swing.JLabel lbCaptionResults;
    private javax.swing.JLabel lbCustomEpochDefInfo;
    private javax.swing.JLabel lbFilename;
    private javax.swing.JLabel lbInputDataformat;
    private javax.swing.JLabel lbMoments;
    private javax.swing.JLabel lbNumberOfDims;
    private javax.swing.JLabel lbNumberOfRestarts;
    private javax.swing.JLabel lbNumberOfSamples;
    private javax.swing.JLabel lbNumberOfStationarySources;
    private javax.swing.JLabel lbParameters;
    private javax.swing.JMenuItem miExit;
    private javax.swing.JMenuItem miLoadEpochDefinitionCSV;
    private javax.swing.JMenuItem miLoadTimeseries;
    private javax.swing.JMenuItem miSaveNonStationaryBasisCSV;
    private javax.swing.JMenuItem miSaveNonstationaryProjectionCSV;
    private javax.swing.JMenuItem miSaveNonstationarySignalsCSV;
    private javax.swing.JMenuItem miSaveResultsCSV;
    private javax.swing.JMenuItem miSaveResultsMatlab;
    private javax.swing.JMenuItem miSaveStationaryBasisCSV;
    private javax.swing.JMenuItem miSaveStationaryProjectionCSV;
    private javax.swing.JMenuItem miSaveStationarySignalsCSV;
    private javax.swing.JPanel panelContents;
    private javax.swing.JPanel panelControl;
    private javax.swing.JPanel panelData;
    private javax.swing.JPanel panelLog;
    private javax.swing.JPanel panelOutputFormat;
    private javax.swing.JPanel panelParameters;
    private javax.swing.JPanel panelResults;
    private javax.swing.JPanel panelSSAMoments;
    private javax.swing.JRadioButton rbChannelsXTime;
    private javax.swing.JRadioButton rbEpochEquallySized;
    private javax.swing.JRadioButton rbTimeXChannels;
    private javax.swing.JRadioButton rbUseCustomEpochDefinition;
    private javax.swing.JScrollPane scrpLog;
    private javax.swing.JTextArea taLog;
    private javax.swing.JTextField tfNumberOfEpochs;
    private javax.swing.JTextField tfNumberOfRestarts;
    private javax.swing.JTextField tfNumberOfStationarySources;
    // End of variables declaration//GEN-END:variables

    /**
     * Makes the GUI visible.
     */
    public void showGUI() {
        setVisible(true);
    }

}

