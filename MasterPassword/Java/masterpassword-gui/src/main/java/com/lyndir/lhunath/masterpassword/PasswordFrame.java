package com.lyndir.lhunath.masterpassword;

import static com.lyndir.lhunath.opal.system.util.StringUtils.*;

import com.lyndir.lhunath.masterpassword.util.Components;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;


/**
 * @author lhunath, 2014-06-08
 */
public class PasswordFrame extends JFrame implements DocumentListener {

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final User                     user;
    private final JPanel                   root;
    private final JTextField               siteNameField;
    private final JComboBox<MPElementType> siteTypeField;
    private final JSpinner                 siteCounterField;
    private final JLabel                   passwordLabel;

    public PasswordFrame(User user)
            throws HeadlessException {
        super( "Master Password" );
        this.user = user;

        JLabel label;

        setDefaultCloseOperation( DISPOSE_ON_CLOSE );
        setContentPane( root = new JPanel( new BorderLayout( 20, 20 ) ) );
        root.setBorder( new EmptyBorder( 20, 20, 20, 20 ) );

        // User
        add( new JLabel( strf( "Generating passwords for: %s", user.getName() ) ), BorderLayout.NORTH );

        // Site
        JPanel sitePanel = new JPanel();
        sitePanel.setLayout( new BoxLayout( sitePanel, BoxLayout.PAGE_AXIS ) );
        sitePanel.setBorder( new CompoundBorder( new EtchedBorder( EtchedBorder.RAISED ), new EmptyBorder( 8, 8, 8, 8 ) ) );
        add( sitePanel, BorderLayout.CENTER );

        // Site Name
        sitePanel.add( new JLabel( "Site Name:", JLabel.LEADING ) );
        sitePanel.add( siteNameField = new JTextField() {
            @Override
            public Dimension getMaximumSize() {
                return new Dimension( Integer.MAX_VALUE, getPreferredSize().height );
            }
        } );
        siteNameField.getDocument().addDocumentListener( this );
        siteNameField.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                updatePassword( new PasswordCallback() {
                    @Override
                    public void passwordGenerated(final String siteName, final String sitePassword) {
                        StringSelection clipboardContents = new StringSelection( sitePassword );
                        Toolkit.getDefaultToolkit().getSystemClipboard().setContents( clipboardContents, null );

                        SwingUtilities.invokeLater( new Runnable() {
                            @Override
                            public void run() {
                                dispose();
                            }
                        } );
                    }
                } );
            }
        } );

        // Site Type & Counter
        sitePanel.add( Components.boxLayout( BoxLayout.LINE_AXIS, //
                                             siteTypeField = new JComboBox<>( MPElementType.values() ),
                                             siteCounterField = new JSpinner( new SpinnerNumberModel( 1, 1, Integer.MAX_VALUE, 1 ) ) {
                                                 @Override
                                                 public Dimension getMaximumSize() {
                                                     return new Dimension( 50, getPreferredSize().height );
                                                 }
                                             } ) );
        siteTypeField.setSelectedItem( MPElementType.GeneratedLong );
        siteCounterField.addChangeListener( new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                updatePassword( null );
            }
        } );

        // Password
        add( passwordLabel = new JLabel( " ", JLabel.CENTER ), BorderLayout.SOUTH );
        passwordLabel.setFont( passwordLabel.getFont().deriveFont( 40f ) );

        pack();
        setMinimumSize( getSize() );
        setPreferredSize( new Dimension( 600, getSize().height ) );
        pack();

        setLocationByPlatform( true );
        setLocationRelativeTo( null );
    }

    private void updatePassword(final PasswordCallback callback) {
        final MPElementType siteType = (MPElementType) siteTypeField.getSelectedItem();
        final String siteName = siteNameField.getText();
        final int siteCounter = (Integer) siteCounterField.getValue();

        executor.submit( new Runnable() {
            @Override
            public void run() {
                final String sitePassword = MasterPassword.generateContent( siteType, siteName, user.getKey(), siteCounter );
                if (callback != null) {
                    callback.passwordGenerated( siteName, sitePassword );
                }

                SwingUtilities.invokeLater( new Runnable() {
                    @Override
                    public void run() {
                        passwordLabel.setText( sitePassword );
                    }
                } );
            }
        } );
    }

    @Override
    public void insertUpdate(final DocumentEvent e) {
        updatePassword( null );
    }

    @Override
    public void removeUpdate(final DocumentEvent e) {
        updatePassword( null );
    }

    @Override
    public void changedUpdate(final DocumentEvent e) {
        updatePassword( null );
    }

    interface PasswordCallback {

        void passwordGenerated(String siteName, String sitePassword);
    }
}
