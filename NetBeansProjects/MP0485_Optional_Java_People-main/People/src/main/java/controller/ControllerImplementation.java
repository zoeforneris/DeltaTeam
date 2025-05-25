package controller;

import model.entity.Person;
import model.entity.PersonException;
import model.dao.DAOArrayList;
import model.dao.DAOFile;
import model.dao.DAOFileSerializable;
import model.dao.DAOHashMap;
import model.dao.DAOJPA;
import model.dao.DAOSQL;
import model.dao.IDAO;
import start.Routes;
import view.DataStorageSelection;
import view.Delete;
import view.Insert;
import view.Menu;
import view.Read;
import view.ReadAll;
import view.Update;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.persistence.*;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import model.entity.Admin;
import model.entity.User;
import org.jdatepicker.DateModel;
import utils.Constants;
import utils.UserRoles;
import view.Count;
import view.Login;

/**
 * This class starts the visual part of the application and programs and manages
 * all the events that it can receive from it. For each event received the
 * controller performs an action.
 *
 * @author Francesc Perez
 * @version 1.1.0
 */
public class ControllerImplementation implements IController, ActionListener {

    //Instance variables used so that both the visual and model parts can be 
    //accessed from the Controller.
    private DataStorageSelection dSS;
    private IDAO dao;
    private Menu menu;
    private Insert insert;
    private Read read;
    private Delete delete;
    private Update update;
    private ReadAll readAll;
    private final Login login;
    private String currentRole;

    /**
     * This constructor allows the controller to know which data storage option
     * the user has chosen.Schedule an event to deploy when the user has made
     * the selection.
     *
     * @param dSS
     */
//    public ControllerImplementation(DataStorageSelection dSS) {
//        this.dSS = dSS;
//        ((JButton) (dSS.getAccept()[0])).addActionListener(this);
//    }
// 
    public ControllerImplementation(Login login) {
        this.login = login;
        login.getAccept().addActionListener(this); // Need to expose this button

    }

    /**
     * With this method, the application is started, asking the user for the
     * chosen storage system.
     */
    @Override
    public void start() {
        try {
            // Initialize with SQL DAO for authentication
//            setupSQLDatabase();
            dao = new DAOSQL();
            login.setVisible(true);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Failed to initialize database connection", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    /**
     * This receives method handles the events of the visual part. Each event
     * has an associated action.
     *
     * @param e The event generated in the visual part
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == login.getAccept()) {
            handleLoginAction();
        } else if (e.getSource() == dSS.getAccept()[0]) {
            handleDataStorageSelection();
        } else if (e.getSource() == menu.getInsert()) {
            handleInsertAction();
        } else if (insert != null && e.getSource() == insert.getInsert()) {
            handleInsertPerson();
        } else if (e.getSource() == menu.getRead()) {
            handleReadAction();
        } else if (read != null && e.getSource() == read.getRead()) {
            handleReadPerson();
        } else if (e.getSource() == menu.getDelete()) {
            handleDeleteAction();
        } else if (delete != null && e.getSource() == delete.getDelete()) {
            handleDeletePerson();
        } else if (e.getSource() == menu.getUpdate()) {
            handleUpdateAction();
        } else if (update != null && e.getSource() == update.getRead()) {
            handleReadForUpdate();
        } else if (update != null && e.getSource() == update.getUpdate()) {
            handleUpdatePerson();
        } else if (e.getSource() == menu.getReadAll()) {
            handleReadAll();
        } else if (e.getSource() == menu.getDeleteAll()) {
            handleDeleteAll();
        } else if (e.getSource() == menu.getCount()) {
            handleCountPeople();
        }
    }

    private void handleDataStorageSelection() {
        String daoSelected = ((javax.swing.JCheckBox) (dSS.getAccept()[1])).getText();
        dSS.dispose();
        switch (daoSelected) {
            case Constants.AL:
                dao = new DAOArrayList();
                break;
            case Constants.HM:
                dao = new DAOHashMap();
                break;
            case Constants.FILE:
                setupFileStorage();
                break;
            case Constants.FILES:
                setupFileSerialization();
                break;
            case Constants.SQL:
                setupSQLDatabase();
                break;
            case Constants.JPA:
                setupJPADatabase();
                break;
        }
        setupMenu();
    }

    private void setupFileStorage() {
        File folderPath = new File(Routes.FILE.getFolderPath());
        File folderPhotos = new File(Routes.FILE.getFolderPhotos());
        File dataFile = new File(Routes.FILE.getDataFile());
        folderPath.mkdir();
        folderPhotos.mkdir();
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(dSS, "File structure not created. Closing application.", "File - People v1.1.0", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
        }
        dao = new DAOFile();
    }

    private void setupFileSerialization() {
        File folderPath = new File(Routes.FILES.getFolderPath());
        File dataFile = new File(Routes.FILES.getDataFile());
        folderPath.mkdir();
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(dSS, "File structure not created. Closing application.", "FileSer - People v1.1.0", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
        }
        dao = new DAOFileSerializable();
    }

    private void setupSQLDatabase() {
        try {
            Connection conn = DriverManager.getConnection(Routes.DB.getDbServerAddress() + Routes.DB.getDbServerComOpt(),
                    Routes.DB.getDbServerUser(), Routes.DB.getDbServerPassword());
//            Connection conn = DriverManager.getConnection(
//                    "jdbc:mysql://localhost:3306/mysql" + Routes.DB.getDbServerComOpt(),
//                    Routes.DB.getDbServerUser(),
//                    Routes.DB.getDbServerPassword()
//            );
            if (conn != null) {
                Statement stmt = conn.createStatement();
                stmt.executeUpdate("create database if not exists " + Routes.DB.getDbServerDB() + ";");
                stmt.executeUpdate("create table if not exists " + Routes.DB.getDbServerDB() + "." + Routes.DB.getDbServerTABLE() + "("
                        + "nif varchar(9) primary key not null, "
                        + "name varchar(50), "
                        + "dateOfBirth DATE, "
                        + "photo varchar(200) );");
                
                stmt.executeUpdate("create table if not exists " + Routes.DB2.getDbServerDB() + "." + Routes.DB2.getDbServerTABLE() + "("
                        + "username varchar(50) primary key, "
                        + "password varchar(100) not null );");
        
                stmt.executeUpdate("create table if not exists " + Routes.DB3.getDbServerDB() + "." + Routes.DB3.getDbServerTABLE() + "("
                        + "username varchar(50) primary key, "
                        + "password varchar(100) not null );");

                stmt.executeUpdate("insert into " + Routes.DB2.getDbServerDB() + "." + Routes.DB2.getDbServerTABLE()
                        + " (username, password) values ('zoef', '1234');");

                stmt.executeUpdate("insert into " + Routes.DB3.getDbServerDB() + "." + Routes.DB3.getDbServerTABLE()
                        + " (username, password) values ('zoeadmin', '1010');");
                stmt.close();
                conn.close();
            }
        } catch (SQLException ex) {
             ex.printStackTrace();
            JOptionPane.showMessageDialog(dSS, "SQL-DDBB structure not created. Closing application.", "SQL_DDBB - People v1.1.0", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
        dao = new DAOSQL();
    }

    private void setupJPADatabase() {
        try {
            EntityManagerFactory emf = Persistence.createEntityManagerFactory(Routes.DBO.getDbServerAddress());
            EntityManager em = emf.createEntityManager();
            em.close();
            emf.close();
        } catch (PersistenceException ex) {
            JOptionPane.showMessageDialog(dSS, "JPA_DDBB not created. Closing application.", "JPA_DDBB - People v1.1.0", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
        dao = new DAOJPA();
    }

    private void setupMenu() {
        menu = new Menu();
        configureMenuBasedOnRole();
        menu.setVisible(true);
        
        menu.getInsert().addActionListener(this);
        menu.getRead().addActionListener(this);
        menu.getUpdate().addActionListener(this);
        menu.getDelete().addActionListener(this);
        menu.getReadAll().addActionListener(this);
        menu.getDeleteAll().addActionListener(this);
        menu.getCount().addActionListener(this);
    }
    
    private void configureMenuBasedOnRole(){
        boolean isAdmin = UserRoles.ADMIN.equals(currentRole);
        
        menu.getInsert().setEnabled(isAdmin);
        menu.getUpdate().setEnabled(isAdmin);
        menu.getDelete().setEnabled(isAdmin);
        menu.getDeleteAll().setEnabled(isAdmin);
        
        
        menu.getRead().setEnabled(true);
        menu.getReadAll().setEnabled(true);
        menu.getCount().setEnabled(true);
        
        menu.setTitle("Menu - People v1.1.( "+ currentRole +" )");
        
    }


    private void handleInsertAction() {
        if (!validateAdminAccess()) return;
        insert = new Insert(menu, true);
        insert.getInsert().addActionListener(this);
        insert.setVisible(true);
    }

    private void handleInsertPerson() {
        Person p = new Person(insert.getNam().getText(), insert.getNif().getText());
        if (insert.getDateOfBirth().getModel().getValue() != null) {
            p.setDateOfBirth(((GregorianCalendar) insert.getDateOfBirth().getModel().getValue()).getTime());
        }
        if (insert.getPhoto().getIcon() != null) {
            p.setPhoto((ImageIcon) insert.getPhoto().getIcon());
        }

        if (!insert.verifyEmail(insert.getEmail().getText())) {
            return;
        }
        p.setEmail(insert.getEmail().getText());

        if (!insert.verifyPhoneNumber(insert.getPhoneNumber().getText())) {
            return;
        }
        p.setPhoneNumber(insert.getPhoneNumber().getText());
        p.setPostalCode(insert.getEmail().getText());
        if (!insert.verifyPC(insert.getPostalCode().getText())) {
            return;
        }
        p.setPostalCode(insert.getPostalCode().getText());

        try {
            insert(p);
            JOptionPane.showMessageDialog(insert, "Person inserted successfully!", "Insert - People v1.1.0", JOptionPane.INFORMATION_MESSAGE);
            insert.getReset().doClick();
        } catch (Exception ex) {
        }
    }

    private void handleReadAction() {
        read = new Read(menu, true);
        read.getRead().addActionListener(this);
        read.setVisible(true);
    }

    private void handleReadPerson() {
        Person p = new Person(read.getNif().getText());
        Person pNew = read(p);
        if (pNew != null) {
            read.getNam().setText(pNew.getName());
            if (pNew.getDateOfBirth() != null) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(pNew.getDateOfBirth());
                DateModel<Calendar> dateModel = (DateModel<Calendar>) read.getDateOfBirth().getModel();
                dateModel.setValue(calendar);
            }
            //To avoid charging former images
            if (pNew.getPhoto() != null) {
                pNew.getPhoto().getImage().flush();
                read.getPhoto().setIcon(pNew.getPhoto());
            }
            if (pNew.getEmail() != null) {
                read.getEmailField().setText(pNew.getEmail());
            }

            if (pNew.getPhoneNumber() != null) {
                read.getPhoneNumber().setText(pNew.getPhoneNumber());
            }
            if (pNew.getPostalCode() != null) {
                read.getPcField().setText(pNew.getPostalCode());
            }
        } else {
            JOptionPane.showMessageDialog(read, p.getNif() + " doesn't exist.", read.getTitle(), JOptionPane.WARNING_MESSAGE);
            read.getReset().doClick();
        }
    }

    public void handleDeleteAction() {
        if (!validateAdminAccess()) return;
        delete = new Delete(menu, true);
        delete.getDelete().addActionListener(this);
        delete.setVisible(true);
    }

    public void handleDeletePerson() {

        if (delete != null) {
            int confirm = JOptionPane.showConfirmDialog(insert, "Are you sure you want to delete this person?", "Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                Person p = new Person(delete.getNif().getText());

                try {
                    delete(p);
                    JOptionPane.showMessageDialog(insert, "Person deleted successfully!", "Delete - People v1.1.0", JOptionPane.INFORMATION_MESSAGE);
                    delete.getReset().doClick();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(insert, "An error occurred while deleting the person.", "Error", JOptionPane.ERROR_MESSAGE);

                }
            }
        }
    }

    public void handleUpdateAction() {
        update = new Update(menu, true);
        update.getUpdate().addActionListener(this);
        update.getRead().addActionListener(this);
        update.setVisible(true);
    }

    public void handleReadForUpdate() {
        if (update != null) {
            Person p = new Person(update.getNif().getText());
            Person pNew = read(p);
            if (pNew != null) {
                update.getNam().setEnabled(true);
                update.getDateOfBirth().setEnabled(true);
                update.getPhoto().setEnabled(true);
                update.getUpdate().setEnabled(true);
                update.getPostalCode().setEnabled(true);
                update.getNam().setText(pNew.getName());
                if (pNew.getDateOfBirth() != null) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(pNew.getDateOfBirth());
                    DateModel<Calendar> dateModel = (DateModel<Calendar>) update.getDateOfBirth().getModel();
                    dateModel.setValue(calendar);
                }
                if (pNew.getPhoto() != null) {
                    pNew.getPhoto().getImage().flush();
                    update.getPhoto().setIcon(pNew.getPhoto());
                    update.getUpdate().setEnabled(true);

                }
                if (pNew.getEmail() != null) {
                    update.getEmail().setText(pNew.getEmail());
                    update.getEmail().setEnabled(true);
                }

                if (pNew.getPhoneNumber() != null) {
                    update.getPhoneNumber().setText(pNew.getPhoneNumber());
                    update.getPhoneNumber().setEnabled(true);
                }
                if (pNew.getPostalCode() != null) {
                    update.getPostalCode().setText(pNew.getPostalCode());
                    update.getUpdate().setEnabled(true);
                }
            } else {
                JOptionPane.showMessageDialog(update, p.getNif() + " doesn't exist.", update.getTitle(), JOptionPane.WARNING_MESSAGE);
                update.getReset().doClick();
            }
        }
    }

    public void handleUpdatePerson() {
        if (update != null) {
            Person p = new Person(update.getNam().getText(), update.getNif().getText());
            if ((update.getDateOfBirth().getModel().getValue()) != null) {
                p.setDateOfBirth(((GregorianCalendar) update.getDateOfBirth().getModel().getValue()).getTime());
            }
            if ((ImageIcon) (update.getPhoto().getIcon()) != null) {
                p.setPhoto((ImageIcon) update.getPhoto().getIcon());
            }
            if (update.getPostalCode().getText() != null) {
                p.setPostalCode(update.getPostalCode().getText());
            }

            if (!insert.verifyEmail(update.getEmail().getText())) {
                return;
            }
            p.setEmail(update.getEmail().getText());

            if (!insert.verifyPhoneNumber(update.getPhoneNumber().getText())) {
                return;
            }
            p.setPhoneNumber(update.getPhoneNumber().getText());

            try {
                update(p);
                JOptionPane.showMessageDialog(insert, "Person updated successfully!", "Update - People v1.1.0", JOptionPane.INFORMATION_MESSAGE);
                update.getReset().doClick();
            } catch (Exception ex) {
            }

        }
    }

    public void handleCountPeople() {
        int count = readAll().size();
        Count countDialog = new Count(menu, true, count);
        countDialog.setVisible(true);

    }

    public void handleReadAll() {
        ArrayList<Person> s = readAll();
        if (s.isEmpty()) {
            JOptionPane.showMessageDialog(menu, "There are not people registered yet.", "Read All - People v1.1.0", JOptionPane.WARNING_MESSAGE);
        } else {
            readAll = new ReadAll(menu, true);
            DefaultTableModel model = (DefaultTableModel) readAll.getTable().getModel();
            for (int i = 0; i < s.size(); i++) {
                model.addRow(new Object[i]);
                model.setValueAt(s.get(i).getNif(), i, 0);
                model.setValueAt(s.get(i).getName(), i, 1);
                if (s.get(i).getDateOfBirth() != null) {
                    model.setValueAt(s.get(i).getDateOfBirth().toString(), i, 2);
                } else {
                    model.setValueAt("", i, 2);
                }
                if (s.get(i).getPhoto() != null) {
                    model.setValueAt("yes", i, 3);
                } else {
                    model.setValueAt("no", i, 3);
                }
                if (s.get(i).getEmail() != null) {
                    model.setValueAt(s.get(i).getEmail(), i, 4);
                }
                if (s.get(i).getPostalCode() != null) {
                    model.setValueAt(s.get(i).getPostalCode(), i, 5);
                    }
                if (s.get(i).getPhoneNumber() != null) {
                    model.setValueAt(s.get(i).getPhoneNumber(), i, 6);
                }
            }
            readAll.setVisible(true);
        }
    }

    public void handleDeleteAll() {
        Object[] options = {"Yes", "No"};
        int answer = JOptionPane.showOptionDialog(menu, "Are you sure you want to delete all registered people?", "Delete All - People v1.1.0", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]);

        if (answer == 0) {
            try {
                deleteAll();
                JOptionPane.showMessageDialog(insert, "All people deleted successfully!", "DeleteAll - People v1.1.0", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(insert, "An error occurred while deleting the person.", "Error", JOptionPane.ERROR_MESSAGE);

            }

        }
    }

    public void handleLoginAction() {

        String username = login.getUsername().getText();
        String password = new String(login.getPassword().getPassword());

        try {

            Admin a = new Admin(username);
            User u = new User(username);
            Admin authenticatedPerson = null;
            User authenticatedPerson2 = null;

            // First try to read as Admin
            authenticatedPerson = ((DAOSQL) dao).readAdmin(a);

            if (authenticatedPerson != null) {
                if ((authenticatedPerson).getPassword().equals(password)) {
                    currentRole = UserRoles.ADMIN;
                    JOptionPane.showMessageDialog(login, "Admin login successful!", "Login", JOptionPane.INFORMATION_MESSAGE);
                    login.dispose();
                    showDataStorageSelection();
                    return;
                }
            } else {

                authenticatedPerson2 = ((DAOSQL) dao).readUser(u);
                if (authenticatedPerson2 != null && ((User) authenticatedPerson2).getPassword().equals(password)) {
                    currentRole = UserRoles.USER;
                    // User login successful
                    JOptionPane.showMessageDialog(login, "User login successful!", "Login", JOptionPane.INFORMATION_MESSAGE);
                    login.dispose();
                    showDataStorageSelection();
                    return;
                }
            }
                // If neither worked
            JOptionPane.showMessageDialog(login, "Invalid username or password", "Login Error", JOptionPane.ERROR_MESSAGE);

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(login, "Database error: " + ex.getMessage(), "Login Error", JOptionPane.ERROR_MESSAGE);
            Logger.getLogger(ControllerImplementation.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private boolean validateAdminAccess() {
        if (!UserRoles.ADMIN.equals(currentRole)) {
           JOptionPane.showMessageDialog(menu, "You don't have permission to perform this action","Access Denied", JOptionPane.WARNING_MESSAGE);
           return false;
        }
        return true;
    }
    
    private void showDataStorageSelection() {
        dSS = new DataStorageSelection();
        ((JButton) dSS.getAccept()[0]).addActionListener(this);
        dSS.setVisible(true);
        dSS.setTitle("Select Storage -("+ currentRole + ")");
    }

    /**
     * This function inserts the Person object with the requested NIF, if it
     * doesn't exist. If there is any access problem with the storage device,
     * the program stops.
     *
     * @param p Person to insert
     */
    @Override
    public void insert(Person p) {
        try {
            if (dao.read(p) == null) {
                dao.insert(p);
            } else {
                throw new PersonException(p.getNif() + " is registered and can not "
                        + "be INSERTED.");
            }
        } catch (Exception ex) {
            //Exceptions generated by file read/write access. If something goes 
            // wrong the application closes.
            if (ex instanceof FileNotFoundException || ex instanceof IOException
                    || ex instanceof ParseException || ex instanceof ClassNotFoundException
                    || ex instanceof SQLException || ex instanceof PersistenceException) {
                JOptionPane.showMessageDialog(insert, ex.getMessage() + ex.getClass() + " Closing application.", insert.getTitle(), JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
            if (ex instanceof PersonException) {
                JOptionPane.showMessageDialog(insert, ex.getMessage(), insert.getTitle(), JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    /**
     * This function updates the Person object with the requested NIF, if it
     * doesn't exist. NIF can not be aupdated. If there is any access problem
     * with the storage device, the program stops.
     *
     * @param p Person to update
     */
    @Override
    public void update(Person p) {
        try {
            dao.update(p);
        } catch (Exception ex) {
            //Exceptions generated by file read/write access. If something goes 
            // wrong the application closes.
            if (ex instanceof FileNotFoundException || ex instanceof IOException
                    || ex instanceof ParseException || ex instanceof ClassNotFoundException
                    || ex instanceof SQLException || ex instanceof PersistenceException) {
                JOptionPane.showMessageDialog(update, ex.getMessage() + ex.getClass() + " Closing application.", update.getTitle(), JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
        }
    }

    /**
     * This function deletes the Person object with the requested NIF, if it
     * exists. If there is any access problem with the storage device, the
     * program stops.
     *
     * @param p Person to read
     */
    @Override
    public void delete(Person p) {
        try {
            if (dao.read(p) != null) {
                dao.delete(p);
            } else {
                throw new PersonException(p.getNif() + " is not registered and can not "
                        + "be DELETED");
            }
        } catch (Exception ex) {
            //Exceptions generated by file, DDBB read/write access. If something  
            //goes wrong the application closes.
            if (ex instanceof FileNotFoundException || ex instanceof IOException
                    || ex instanceof ParseException || ex instanceof ClassNotFoundException
                    || ex instanceof SQLException || ex instanceof PersistenceException) {
                JOptionPane.showMessageDialog(read, ex.getMessage() + ex.getClass() + " Closing application.", "Insert - People v1.1.0", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
            if (ex instanceof PersonException) {
                JOptionPane.showMessageDialog(read, ex.getMessage(), "Delete - People v1.1.0", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    /**
     * This function returns the Person object with the requested NIF, if it
     * exists. Otherwise it returns null. If there is any access problem with
     * the storage device, the program stops.
     *
     * @param p Person to read
     * @return Person or null
     */
    @Override
    public Person read(Person p) {
        try {
            Person pTR = dao.read(p);
            if (pTR != null) {
                return pTR;
            }
        } catch (Exception ex) {

            //Exceptions generated by file read access. If something goes wrong 
            //reading the file, the application closes.
            if (ex instanceof FileNotFoundException || ex instanceof IOException
                    || ex instanceof ParseException || ex instanceof ClassNotFoundException
                    || ex instanceof SQLException || ex instanceof PersistenceException) {
                JOptionPane.showMessageDialog(read, ex.getMessage() + " Closing application.", read.getTitle(), JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
        }
        return null;
    }

    /**
     * This function returns the people registered. If there is any access
     * problem with the storage device, the program stops.
     *
     * @return ArrayList
     */
    @Override
    public ArrayList<Person> readAll() {
        ArrayList<Person> people = new ArrayList<>();
        try {
            people = dao.readAll();
        } catch (Exception ex) {
            if (ex instanceof FileNotFoundException || ex instanceof IOException
                    || ex instanceof ParseException || ex instanceof ClassNotFoundException
                    || ex instanceof SQLException || ex instanceof PersistenceException) {
                JOptionPane.showMessageDialog(readAll, ex.getMessage() + " Closing application.", readAll.getTitle(), JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
        }
        return people;
    }

    /**
     * This function deletes all the people registered. If there is any access
     * problem with the storage device, the program stops.
     */
    @Override
    public void deleteAll() {
        try {
            dao.deleteAll();
        } catch (Exception ex) {
            if (ex instanceof FileNotFoundException || ex instanceof IOException
                    || ex instanceof ParseException || ex instanceof ClassNotFoundException
                    || ex instanceof SQLException || ex instanceof PersistenceException) {
                JOptionPane.showMessageDialog(menu, ex.getMessage() + " Closing application.", "Delete All - People v1.1.0", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
        }
    }

    @Override
    public void count() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

}
