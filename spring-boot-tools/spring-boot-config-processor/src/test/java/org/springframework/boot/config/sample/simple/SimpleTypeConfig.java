/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.config.sample.simple;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Expose simple types to make sure these are detected properly.
 *
 * @author Stephane Nicoll
 */
@ConfigurationProperties(prefix = "simple.type")
public class SimpleTypeConfig {

	private String myString;

	private Byte myByte;

	private byte myPrimitiveByte;

	private Character myChar;

	private char myPrimitiveChar;

	private Boolean myBoolean;

	private boolean myPrimitiveBoolean;

	private Short myShort;

	private short myPrimitiveShort;

	private Integer myInteger;

	private int myPrimitiveInteger;

	private Long myLong;

	private long myPrimitiveLong;

	private Double myDouble;

	private double myPrimitiveDouble;

	private Float myFloat;

	private float myPrimitiveFloat;

	public String getMyString() {
		return myString;
	}

	public void setMyString(String myString) {
		this.myString = myString;
	}

	public Byte getMyByte() {
		return myByte;
	}

	public void setMyByte(Byte myByte) {
		this.myByte = myByte;
	}

	public byte getMyPrimitiveByte() {
		return myPrimitiveByte;
	}

	public void setMyPrimitiveByte(byte myPrimitiveByte) {
		this.myPrimitiveByte = myPrimitiveByte;
	}

	public Character getMyChar() {
		return myChar;
	}

	public void setMyChar(Character myChar) {
		this.myChar = myChar;
	}

	public char getMyPrimitiveChar() {
		return myPrimitiveChar;
	}

	public void setMyPrimitiveChar(char myPrimitiveChar) {
		this.myPrimitiveChar = myPrimitiveChar;
	}

	public Boolean getMyBoolean() {
		return myBoolean;
	}

	public void setMyBoolean(Boolean myBoolean) {
		this.myBoolean = myBoolean;
	}

	public boolean isMyPrimitiveBoolean() {
		return myPrimitiveBoolean;
	}

	public void setMyPrimitiveBoolean(boolean myPrimitiveBoolean) {
		this.myPrimitiveBoolean = myPrimitiveBoolean;
	}

	public Short getMyShort() {
		return myShort;
	}

	public void setMyShort(Short myShort) {
		this.myShort = myShort;
	}

	public short getMyPrimitiveShort() {
		return myPrimitiveShort;
	}

	public void setMyPrimitiveShort(short myPrimitiveShort) {
		this.myPrimitiveShort = myPrimitiveShort;
	}

	public Integer getMyInteger() {
		return myInteger;
	}

	public void setMyInteger(Integer myInteger) {
		this.myInteger = myInteger;
	}

	public int getMyPrimitiveInteger() {
		return myPrimitiveInteger;
	}

	public void setMyPrimitiveInteger(int myPrimitiveInteger) {
		this.myPrimitiveInteger = myPrimitiveInteger;
	}

	public Long getMyLong() {
		return myLong;
	}

	public void setMyLong(Long myLong) {
		this.myLong = myLong;
	}

	public long getMyPrimitiveLong() {
		return myPrimitiveLong;
	}

	public void setMyPrimitiveLong(long myPrimitiveLong) {
		this.myPrimitiveLong = myPrimitiveLong;
	}

	public Double getMyDouble() {
		return myDouble;
	}

	public void setMyDouble(Double myDouble) {
		this.myDouble = myDouble;
	}

	public double getMyPrimitiveDouble() {
		return myPrimitiveDouble;
	}

	public void setMyPrimitiveDouble(double myPrimitiveDouble) {
		this.myPrimitiveDouble = myPrimitiveDouble;
	}

	public Float getMyFloat() {
		return myFloat;
	}

	public void setMyFloat(Float myFloat) {
		this.myFloat = myFloat;
	}

	public float getMyPrimitiveFloat() {
		return myPrimitiveFloat;
	}

	public void setMyPrimitiveFloat(float myPrimitiveFloat) {
		this.myPrimitiveFloat = myPrimitiveFloat;
	}
}
