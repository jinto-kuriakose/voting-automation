/**
 * 
 */
package com.test.votelink;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
	private static final int endDay = 3;

	private static final String TIME_CONFIG_HELP = "For Example, if you want to get reminded on every day at 10:10 AM. Give minute as 10 and hour as 10";

	// private static final String CLICKS = "clicks";

	private static boolean populateUserDetails = true;
	private static boolean voteForMe = false;

	private static Logger logger = LoggerFactory
			.getLogger(VoteForLinkReminder.class);

	public static void main(String[] args) {
		Calendar calendarNow = Calendar.getInstance();
		Calendar endCalendar = Calendar.getInstance();
		endCalendar.set(Calendar.DAY_OF_MONTH, endDay);
		endCalendar.set(Calendar.MONTH, endMonth);
		endCalendar.set(Calendar.HOUR_OF_DAY, 0);
		endCalendar.set(Calendar.MINUTE, 0);
		endCalendar.set(Calendar.SECOND, 0);
		logger.info("Voting Date = " + calendarNow.getTime());
		logger.info("Configured Voting End Date = " + endCalendar.getTime());

		if (calendarNow.before(endCalendar)) {
			String startVote = System.getProperty("startVote");
			if ("true".equalsIgnoreCase(startVote)) {
				logger.info("Going to vote on " + calendarNow.getTime());
				populateUserCredentialsFromPropertyFile();
				startVoting();
			} else {
				getUserDetails();
			}
		} else {
			logger.info("**** Voting ended ****");
			endVoting();
		}
	}

	private static void endVoting() {

		String command = "crontab -r";
		ProcessBuilder pb = new ProcessBuilder("sh", "-c", command);
		try {
			pb.start();
		} catch (IOException e) {
			logger.error("", e);
		}
		try {
			File getVoteForLinkHomeLocation = new File(
					getVoteForLinkHomeLocation());
			if (getVoteForLinkHomeLocation.exists()) {
				deleteDir(getVoteForLinkHomeLocation);
			}
		} catch (Exception ex) {
			logger.error("", ex);
		}
		JOptionPane.showMessageDialog(null, "Link Voting Ended. Thank you!.");
	}

	private static void deleteDir(File file) {
		File[] contents = file.listFiles();
		if (contents != null) {
			for (File f : contents) {
				deleteDir(f);
			}
		}
		file.delete();
	}

	private static void populateUserCredentialsFromPropertyFile() {
		File configFilePath = new File(getConfigFileLocation());
		Properties properties = new Properties();
		try {
			properties.load(new FileReader(configFilePath));
			userName = properties.getProperty(USERNAME_KEY);
			emailId = properties.getProperty(EMAIL_KEY);

			String populateUserDetailsString = properties
					.getProperty(POPULATE_DETAILS_KEY);
			// String voteForMeString = properties.getProperty(VOTE_FOR_ME_KEY);

			populateUserDetails = Boolean
					.parseBoolean(populateUserDetailsString);
			// voteForMe = Boolean.parseBoolean(voteForMeString);

		} catch (IOException e) {
			logger.error("", e);
		}
	}

	private static String getVoteForLinkHomeLocation() {
		return getUserHomeDirectory() + "/" + APPNAME;
	}

	private static String getConfigFileLocation() {
		return getVoteForLinkHomeLocation() + "/" + CONFIG_FILE;
	}

	private static String getUserHomeDirectory() {
		String userHomeDirectory = System.getProperty("user.home");
		return userHomeDirectory;
	}

	private static void setUpCronJob() {
		String userHomeDirectory = getUserHomeDirectory();
		File jarPath = new File(getJarInstallationLocation());

		if (null == minutes) {
			minutes = "10";
		}
		if (hour == null) {
			hour = "10";
		}

		minutes = minutes.trim();
		hour = hour.trim();

		String exp = minutes + " " + hour
				+ " * 7,8,9 * java -DstartVote=true -jar "
				+ jarPath.getAbsolutePath() + "\n";

		byte[] expByte = exp.getBytes();

		File path = new File(userHomeDirectory + "/" + APPNAME + "/"
				+ CRON_FILE);
		OutputStream out = null;
		try {
			out = new BufferedOutputStream(new FileOutputStream(path));
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
		String command = "crontab " + path.getAbsolutePath().toString();
		ProcessBuilder pb = new ProcessBuilder("sh", "-c", command);
		try {
			pb.start();
		} catch (IOException e) {
			logger.error("", e);
		}
	}

	private static String getJarInstallationLocation() {
		return getVoteForLinkHomeLocation() + "/" + JAR_NAME;
	}

	public static void createVotingHomeDirectory() {
		String userHomeDirectory = getUserHomeDirectory();

		try {
			File userHomeDirectoryPath = new File(userHomeDirectory + "/"
					+ APPNAME);
			userHomeDirectoryPath.mkdir();
		} catch (Exception e1) {
			logger.error("", e1);
		}
	}

	public static void createConfigFile() {

		File file = new File(getConfigFileLocation());
		try {
			file.createNewFile();
		} catch (Exception e1) {
			logger.error("", e1);
		}
		OutputStream out = null;
		try {
			out = new BufferedOutputStream(new FileOutputStream(file));
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

		String currentRelativePath = new File("").getAbsolutePath();
		logger.info("Current relative path is: " + currentRelativePath);

		File source = new File(currentRelativePath + "/" + JAR_NAME);

		File dest = new File(getJarInstallationLocation());
		try {
			dest.createNewFile();
			copyFileUsingStream(source, dest);
		} catch (Exception e) {
			logger.error("", e);
		}
	}

	private static void copyFileUsingStream(File source, File dest)
			throws IOException {
		InputStream is = null;
		OutputStream os = null;
		try {
			is = new FileInputStream(source);
			os = new FileOutputStream(dest);
			byte[] buffer = new byte[1024];
			int length;
			while ((length = is.read(buffer)) > 0) {
				os.write(buffer, 0, length);
			}
		} finally {
			is.close();
			os.close();
		}
	}

	private static void getUserDetails() {
		String heading = "Vote For Link Reminder";
		final JFrame f = new JFrame(heading);
		f.setTitle(heading);

		GridLayout gridLayout = new GridLayout(6, 2, 5, 5);
		JPanel p1 = new JPanel(gridLayout);
		p1.setBackground(Color.WHITE);
		Border border = new LineBorder(Color.LIGHT_GRAY, 10);

		Border margin = new EmptyBorder(30, 30, 30, 30);
		p1.setBorder(new CompoundBorder(border, margin));

		JLabel lblContact = new JLabel(
				"Contact Us : Jinto_Kuriakose@intuit.com");

		JLabel lblName = new JLabel("Your Name :");
		JLabel lblEmail = new JLabel("Your Email :");

		JLabel lblMinute = new JLabel("Minutes (0-59) :");
		JLabel lblHour = new JLabel("Hour (0-23) :");

		// JLabel lblAutoPopulate = new JLabel("Auto populate my details :");
		// JLabel lblVoteForMe = new JLabel("Automatically Vote For Me :");

		final JTextField userNameField = new JTextField();
		final JTextField emailField = new JTextField();
		final JTextField jFieldMinute = new JTextField();
		jFieldMinute.setToolTipText(TIME_CONFIG_HELP);
		final JTextField jFieldHour = new JTextField();
		jFieldHour.setToolTipText(TIME_CONFIG_HELP);
		// final JCheckBox populateUserDetailsCheck = new JCheckBox();
		// final JCheckBox voteForMeCheck = new JCheckBox();
		// voteForMeCheck.setSelected(true);

		p1.add(lblName);
		p1.add(userNameField);

		p1.add(lblEmail);
		p1.add(emailField);

		p1.add(lblMinute);
		p1.add(jFieldMinute);

		p1.add(lblHour);
		p1.add(jFieldHour);

		// p1.add(lblVoteForMe);
		// p1.add(voteForMeCheck);

		JButton button = new JButton();
		button.setText("Submit");
		button.setBorderPainted(true);
		button.setBackground(Color.BLUE);
		button.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				userName = userNameField.getText();
				emailId = emailField.getText();
				// populateUserDetails = populateUserDetailsCheck.isSelected();
				minutes = jFieldMinute.getText();
				hour = jFieldHour.getText();
				// voteForMe = voteForMeCheck.isSelected();

				try {
					createVotingHomeDirectory();
					createConfigFile();
					setupAndCopyInstallation();
					setUpCronJob();
					JOptionPane
							.showMessageDialog(null,
									"Successfully Installed Link Vote Reminder. Thank you!");
				} catch (Exception ex) {
					logger.error("", ex);
					JOptionPane.showMessageDialog(null, ex.getMessage());
				}
				f.dispose();
			}
		});

		final JTextField jFieldHidden = new JTextField();
		jFieldHidden.setVisible(false);
		p1.add(jFieldHidden);
		p1.add(button);

		p1.add(lblContact);

		f.add(p1);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setSize(600, 500);
		f.setLocationRelativeTo(null);
		f.setVisible(true);
	}

	public static void startVoting() {

		FirefoxDriver driver = new FirefoxDriver();
		driver.manage().window().maximize();
		driver.get("http://www.sleeter.com/awesomeapps/2016/intuit-link");
		if (populateUserDetails) {
			List<WebElement> login = driver
					.findElementsByXPath("//button[@data-toggle='modal'][@data-target='#awesome-82']");
			login.get(0).click();

			List<WebElement> nameElement = driver
					.findElementsByXPath("//input[@name='first_name']");

			new WebDriverWait(driver, 4).until(ExpectedConditions
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

}
