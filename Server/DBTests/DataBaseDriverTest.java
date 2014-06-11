import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.voting.database.DataBaseDriver;
import com.voting.database.DataBaseDriverFactory;
import com.voting.database.User;


public class DataBaseDriverTest {
	
	private static DataBaseDriver db;
	private static byte[] id;
	private static byte[] PrK;
	static String name; 
	static String email;
	static byte[] certificate;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		id = new byte[16];
		id[0] = 31;
		PrK = new byte[128];
		PrK[0] = 32;
		name = "Super man";
		email = "Super@mail.ua";
		certificate = new byte[128];
		certificate[0] = 33;
		
		db = DataBaseDriverFactory.getDriver("server.conf");
		db.connect();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}
	
	@Before
	public void setUp() throws Exception {
		User user = new User(id, PrK, name, email, certificate);
		db.insertNewUser(user);
	}

	@After
	public void tearDown() throws Exception {
		db.deleteUser(id);
	}

	@Test
	public void userTrustTest() {
		User user = db.getUser(id);
		
		Assert.assertEquals("User didn't have high trust", "high", user.getTrust());
	}
	
	@Test
	public void userDeletingTest() {
		db.deleteUser(id);
		
		User user = db.getUser(id);
		
		Assert.assertEquals("User wasn't deleted", null, user);	
	}
	
	@Test
	public void insertedUserValidnessTest() {		
		User user = db.getUser(id);
		
		Assert.assertArrayEquals("User id was incorrect", id, user.getId());
		Assert.assertArrayEquals("User private key was incorrect", PrK, user.getPrK());
		Assert.assertEquals("User name was incorrect", name, user.getName());
		Assert.assertEquals("User email was incorrect", email, user.getEmail());
		Assert.assertArrayEquals("User ceritificate was incorrect", certificate, user.getCertificate());
		Assert.assertEquals("User trust level is incorrect", "high", user.getTrust());
	}
	
	@Test
	public void setTrustbyIdTest() {
		db.setTrustById("middle", id);
		
		User user = db.getUser(id);
		
		Assert.assertEquals("Setting trust level for user by id failed", "middle", user.getTrust());
	}
	
	@Test
	public void setBanbyPrKTest() {
		db.setBanByPrK(PrK);
		
		User user = db.getUser(id);
		
		Assert.assertEquals("Setting ban for user by private key failed", "ban", user.getTrust());
	}
}
