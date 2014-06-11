package com.voting.database.nosql;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.dynamodbv2.model.AttributeAction;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.voting.database.DataBaseDriver;
import com.voting.database.User;

public class NoSqlConnector implements DataBaseDriver {

	static AmazonDynamoDBClient client;
	static String tableName = "keytable";
	static String pathFileProperties = "/home/ubuntu/serverdir/AwsCredentials.properties";

	@Override
	public boolean connect() {
		try {
			File fileProperties = new File(pathFileProperties);
			AWSCredentials credentials = new PropertiesCredentials(
					fileProperties);
			client = new AmazonDynamoDBClient(credentials);
			client.setEndpoint("dynamodb.eu-west-1.amazonaws.com/");
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	@Override
	public boolean insertNewUser(User user) {
		try {
			Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();

			item.put("id",
					new AttributeValue().withB(ByteBuffer.wrap(user.getId())));
			item.put("pk",
					new AttributeValue().withB(ByteBuffer.wrap(user.getPrK())));
			item.put("name",
					new AttributeValue().withS(user.getName()));
			item.put("email", 
					new AttributeValue().withS(user.getEmail()));
			item.put("cert", 
					new AttributeValue().withB(ByteBuffer.wrap(user.getCertificate())));
			item.put("trust", 
					new AttributeValue().withS("high"));
			PutItemRequest itemRequest = new PutItemRequest().withTableName(
					tableName).withItem(item);
			client.putItem(itemRequest);
		} catch (AmazonServiceException ase) {
			System.err.println("Failed to create item in " + tableName + " " + ase);
			return false;
		}

		return true;
	}
	
	private void setTrustByIdPrivate(String trust, byte[] id) {
		Map<String, AttributeValueUpdate> updateItems = new HashMap<String, AttributeValueUpdate>();

		HashMap<String, AttributeValue> key = new HashMap<String, AttributeValue>();
		key.put("id", new AttributeValue().withB(ByteBuffer.wrap(id)));

		updateItems.put("trust",
				new AttributeValueUpdate().withAction(AttributeAction.PUT)
						.withValue(new AttributeValue().withS(trust)));

		UpdateItemRequest updateItemRequest = new UpdateItemRequest()
				.withTableName(tableName).withKey(key)
				.withReturnValues(ReturnValue.UPDATED_NEW)
				.withAttributeUpdates(updateItems);

		client.updateItem(updateItemRequest);
	}

	@Override
	public boolean setBanByPrK(byte[] PrK) {
		try {
			QueryRequest queryRequest = new QueryRequest()
		    .withTableName(tableName)
		    .withIndexName("pk-index")
		    .withAttributesToGet("id");

			HashMap<String, Condition> keyConditions = new HashMap<String, Condition>();

			keyConditions.put(
					"pk",
					new Condition().withComparisonOperator(
							ComparisonOperator.EQ).withAttributeValueList(
							new AttributeValue().withB(ByteBuffer.wrap(PrK))));

			queryRequest.setKeyConditions(keyConditions);

			QueryResult result = client.query(queryRequest);

			Iterator<Map<String, AttributeValue>> resultIter = result
					.getItems().iterator();

			while (resultIter.hasNext()) {
				Map<String, AttributeValue> attribs = resultIter.next();
				setTrustByIdPrivate("ban", attribs.get("id").getB().array()); 
			}
		} catch (AmazonServiceException ase) {
			System.err.println("Failed to set ban into " + tableName + " " + ase);
			return false;
		}
		return true;
	}

	@Override
	public boolean setTrustById(String trust, byte[] id) {
		try {
			setTrustByIdPrivate(trust, id);
		} catch (AmazonServiceException ase) {
			System.err.println("Failed to set trust into " + tableName + " "
					+ ase);
			return false;
		}
		return true;
	}

	@Override
	public User getUser(byte[] id) {
		try {
			HashMap<String, AttributeValue> key = new HashMap<String, AttributeValue>();
			key.put("id", new AttributeValue().withB(ByteBuffer.wrap(id)));

			GetItemRequest getItemRequest = new GetItemRequest().withTableName(
					tableName).withKey(key);

			GetItemResult result = client.getItem(getItemRequest);
			Map<String, AttributeValue> map = result.getItem();
			
			if (map == null)
				return null;

			return new User(map.get("id").getB().array(),
							map.get("pk").getB().array(),
							map.get("name").getS(), 
							map.get("email").getS(),
							map.get("cert").getB().array(),
							map.get("trust").getS());
			
		} catch (AmazonServiceException ase) {
			System.err.println("Failed to get item from " + tableName + " "	+ ase);
			return null;
		}
	}

	@Override
	public boolean deleteUser(byte[] id) {
		try {
			HashMap<String, AttributeValue> key = new HashMap<String, AttributeValue>();
			key.put("id", new AttributeValue().withB(ByteBuffer.wrap(id)));

			DeleteItemRequest deleteItemRequest = new DeleteItemRequest()
					.withTableName(tableName).withKey(key);

			client.deleteItem(deleteItemRequest);
			return true;
		} catch (AmazonServiceException ase) {
			System.err.println("Failed to delete item from " + tableName + " "
					+ ase);
			return false;
		}
	}
}
