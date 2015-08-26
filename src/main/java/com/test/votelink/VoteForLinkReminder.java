/**
 * 
 */
package com.test.votelink;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jinto.kuriakose
 *
 */
public class VoteForLinkReminder {

	private static String APPNAME = "VoteForLinkReminder";
	private static String JAR_NAME = "VoteForLinkReminder.jar";
	private static String CONFIG_FILE = "config.ini";
	private static String CRON_FILE = "cron.ini";

	private static String minutes;
	private static String hour;

	private static String userName;
	private static String emailId;

	private static final String USERNAME_KEY = "username";
	private static final String EMAIL_KEY = "email";
	private static final String POPULATE_DETAILS_KEY = "populateUserDetails";
	private static final String VOTE_FOR_ME_KEY = "voteForMe";

	private static final int endMonth = 9;
	private static final int endDay = 1;

	// private static final String CLICKS = "clicks";

	private static boolean populateUserDetails = true;
	private static boolean voteForMe;

	private static Logger logger = LoggerFactory
			.getLogger(VoteForLinkReminder.class);

	public static void main(String[] args) {
		// startVotingChrome();
		String startVote = System.getProperty("startVote");
		if ("true".equalsIgnoreCase(startVote)) {

			Calendar calendarNow = Calendar.getInstance();
			Calendar endCalendar = Calendar.getInstance();
			endCalendar.set(Calendar.DAY_OF_MONTH, endDay);
			endCalendar.set(Calendar.MONTH, endMonth);
			// if (calendarNow.before(endCalendar)) {
			populateUserCredentialsFromPropertyFile();
			startVoting();
			// } else {
			logger.info("Voting ended");
			// Voting is done. Delete application.
			// }
		} else {
			getUserDetails();
		}
	}

	private static void populateUserCredentialsFromPropertyFile() {
		String userHomeDirectory = System.getProperty("user.home");
		Path configFilePath = Paths
				.get(userHomeDirectory, APPNAME, CONFIG_FILE);
		Properties properties = new Properties();
		try {
			properties.load(Files.newInputStream(configFilePath, READ));
			userName = properties.getProperty(USERNAME_KEY);
			emailId = properties.getProperty(EMAIL_KEY);

			String populateUserDetailsString = properties
					.getProperty(POPULATE_DETAILS_KEY);
			String voteForMeString = properties.getProperty(VOTE_FOR_ME_KEY);

			populateUserDetails = Boolean
					.parseBoolean(populateUserDetailsString);
			voteForMe = Boolean.parseBoolean(voteForMeString);

		} catch (IOException e) {
			logger.error("", e);
		}
	}

	private static void setUpCronJob() {
		String userHomeDirectory = System.getProperty("user.home");
		Path jarPath = Paths.get(userHomeDirectory, APPNAME, JAR_NAME);

		if (null == minutes) {
			minutes = "*/2";
		}
		if (hour == null) {
			hour = "*";
		}

		String exp = minutes + " " + hour
				+ " * * * java -DstartVote=true -jar "
				+ jarPath.toAbsolutePath() + "\n";

		byte[] expByte = exp.getBytes();

		Path path = Paths.get(userHomeDirectory, APPNAME, CRON_FILE);
		OutputStream out = null;
		try {
			out = new BufferedOutputStream(Files.newOutputStream(path, CREATE,
					TRUNCATE_EXISTING));
			out.write(expByte, 0, expByte.length);
		} catch (Exception ex) {
			logger.error("", ex);
		} finally {
			if (out != null)
				try {
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}

		String command = "crontab " + path.toAbsolutePath().toString();
		ProcessBuilder pb = new ProcessBuilder("sh", "-c", command);
		try {
			pb.start();
		} catch (IOException e) {
			logger.error("", e);
		}
	}

	public static void createVotingHomeDirectory() {
		String userHomeDirectory = System.getProperty("user.home");
		Path userHomeDirectoryPath = Paths.get(userHomeDirectory, APPNAME);
		try {
			userHomeDirectoryPath = Files
					.createDirectory(userHomeDirectoryPath);
		} catch (IOException e1) {
			logger.error("", e1);
		}
	}

	public static void createConfigFile() {
		String userHomeDirectory = System.getProperty("user.home");

		Path path = Paths.get(userHomeDirectory, APPNAME, CONFIG_FILE);
		try {
			new File(path.toAbsolutePath().toString()).createNewFile();
		} catch (Exception e1) {
			logger.error("", e1);
		}
		// try {
		// path = Files.createFile(path);
		//
		// } catch (FileAlreadyExistsException ex) {
		// ex.printStackTrace();
		// } catch (IOException e) {
		// e.printStackTrace();
		// }
		OutputStream out = null;
		try {
			out = new BufferedOutputStream(Files.newOutputStream(path,
					StandardOpenOption.CREATE, StandardOpenOption.WRITE,
					TRUNCATE_EXISTING));
			StringBuilder builder = new StringBuilder();
			builder.append(USERNAME_KEY).append("=").append(userName)
					.append("\n");
			builder.append(EMAIL_KEY).append("=").append(emailId).append("\n");
			builder.append(POPULATE_DETAILS_KEY).append("=")
					.append(populateUserDetails).append("\n");
			builder.append(VOTE_FOR_ME_KEY).append("=").append(voteForMe)
					.append("\n");

			byte[] data = builder.toString().getBytes();
			out.write(data, 0, data.length);
		} catch (Exception ex) {
			logger.error("", ex);
		} finally {
			if (out != null)
				try {
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}

	}

	public static void setupAndCopyInstallation() {
		String userHomeDirectory = System.getProperty("user.home");
		Path userHomeDirectoryPath = Paths.get(userHomeDirectory, APPNAME);

		try {
			userHomeDirectoryPath = Files
					.createDirectory(userHomeDirectoryPath);
		} catch (IOException e1) {
			logger.error("", e1);
		}

		Path currentRelativePath = Paths.get("");
		String s = currentRelativePath.toAbsolutePath().toString();
		logger.info("Current relative path is: " + s);

		Path sourcePath = FileSystems.getDefault().getPath(
				currentRelativePath.toAbsolutePath().toString(), JAR_NAME);
		Path destinationPath = FileSystems.getDefault().getPath(
				userHomeDirectory + "/" + APPNAME, JAR_NAME);
		try {
			Files.copy(sourcePath, destinationPath,
					StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			logger.error("", e);
		}
	}

	private static void getUserDetails() {
		String heading = "Vote For Link Reminder";
		final JFrame f = new JFrame(heading);
		f.setTitle(heading);

		JPanel p1 = new JPanel();
		p1.setBackground(Color.white);
		GridLayout gridLayout = new GridLayout(6, 2, 5, 5);
		Border border = new LineBorder(Color.LIGHT_GRAY, 10);

		Border margin = new EmptyBorder(30, 30, 30, 30);
		p1.setBorder(new CompoundBorder(border, margin));

		JLabel lblName = new JLabel("User name :");
		JLabel lblEmail = new JLabel("Email :");

		JLabel lblMinute = new JLabel("Minutes (0-59) :");
		JLabel lblHour = new JLabel("Hour (0-24) :");
		// JLabel lblAutoPopulate = new JLabel("Auto populate my details :");
		JLabel lblVoteForMe = new JLabel("Automatically Vote For Me :");

		final JTextField userNameField = new JTextField();
		final JTextField emailField = new JTextField();
		final JTextField jFieldMinute = new JTextField();
		final JTextField jFieldHour = new JTextField();

		// final JCheckBox populateUserDetailsCheck = new JCheckBox();
		final JCheckBox voteForMeCheck = new JCheckBox();
		voteForMeCheck.setSelected(true);

		p1.setLayout(gridLayout);

		p1.add(lblName);
		p1.add(userNameField);

		p1.add(lblEmail);
		p1.add(emailField);

		p1.add(lblMinute);
		p1.add(jFieldMinute);

		p1.add(lblHour);
		p1.add(jFieldHour);

		p1.add(lblVoteForMe);
		p1.add(voteForMeCheck);

		JButton button = new JButton();
		button.setText("Submit");
		button.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				userName = userNameField.getText();
				emailId = emailField.getText();
				// populateUserDetails = populateUserDetailsCheck.isSelected();
				minutes = jFieldMinute.getText();
				hour = jFieldHour.getText();
				voteForMe = voteForMeCheck.isSelected();

				try {
					createVotingHomeDirectory();
					createConfigFile();
					setupAndCopyInstallation();
					setUpCronJob();
					JOptionPane.showMessageDialog(null,
							"Vote For Reminder installed Successfully");
				} catch (Exception ex) {
					logger.error("", ex);
					JOptionPane.showMessageDialog(null, ex.getMessage());
				}
				f.dispose();
			}
		});
		p1.add(button);
		f.add(p1);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setSize(500, 500);
		f.setLocationRelativeTo(null);
		f.setVisible(true);
	}

	public static void startVotingChrome() {
		System.setProperty("webdriver.chrome.driver",
				"/Applications/Google Chrome.app/Contents/MacOS/Google Chrome");

		DesiredCapabilities dc = DesiredCapabilities.chrome();
		String[] switches = { "--ignore-certificate-errors",
				"--disable-popup-blocking", "--disable-translate" };
		dc.setCapability("chrome.switches", Arrays.asList(switches));
		ChromeDriver driver = new ChromeDriver(dc);
		driver.get("http://www.google.com/");
		driver.get("http://www.sleeter.com/awesomeapps/2016/intuit-link");

		if (populateUserDetails) {
			List<WebElement> login = driver
					.findElementsByXPath("//button[@data-toggle='modal'][@data-target='#awesome-82']");
			login.get(0).click();

			List<WebElement> nameElement = driver
					.findElementsByXPath("//input[@name='first_name']");

			new WebDriverWait(driver, 10).until(ExpectedConditions
					.visibilityOf(nameElement.get(0)));

			nameElement.get(0).sendKeys(userName);

			List<WebElement> emailElement = driver
					.findElementsByXPath("//input[@name='email']");
			emailElement.get(0).sendKeys(emailId);

			if (voteForMe) {
				List<WebElement> login2 = driver
						.findElementsByXPath("//button[@style='cursor: pointer;']");
				login2.get(0).click();
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
					logger.error("", e);
				}
				driver.close();
			}
		}
	}

	public static void startVoting() {

		FirefoxDriver driver = new FirefoxDriver();
		driver.get("http://www.sleeter.com/awesomeapps/2016/intuit-link");
		if (populateUserDetails) {
			List<WebElement> login = driver
					.findElementsByXPath("//button[@data-toggle='modal'][@data-target='#awesome-82']");
			login.get(0).click();

			List<WebElement> nameElement = driver
					.findElementsByXPath("//input[@name='first_name']");

			new WebDriverWait(driver, 10).until(ExpectedConditions
					.visibilityOf(nameElement.get(0)));

			nameElement.get(0).sendKeys(userName);

			List<WebElement> emailElement = driver
					.findElementsByXPath("//input[@name='email']");
			emailElement.get(0).sendKeys(emailId);

			if (voteForMe) {
				List<WebElement> login2 = driver
						.findElementsByXPath("//button[@style='cursor: pointer;']");
				login2.get(0).click();
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
					logger.error("", e);
				}
				driver.close();
			}
		}
	}
}
