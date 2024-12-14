/*
 * Copyright (c) 2024 Villu Ruusmann
 *
 * This file is part of JPMML-Evaluator
 *
 * JPMML-Evaluator is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JPMML-Evaluator is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with JPMML-Evaluator.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jpmml.evaluator.rexp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.EvaluatorUtil;
import org.jpmml.evaluator.Table;
import org.jpmml.evaluator.TableReader;
import org.jpmml.evaluator.TableWriter;
import org.jpmml.rexp.RBooleanVector;
import org.jpmml.rexp.RDoubleVector;
import org.jpmml.rexp.RExp;
import org.jpmml.rexp.RExpParser;
import org.jpmml.rexp.RExpWriter;
import org.jpmml.rexp.RGenericVector;
import org.jpmml.rexp.RIntegerVector;
import org.jpmml.rexp.RPair;
import org.jpmml.rexp.RString;
import org.jpmml.rexp.RStringVector;
import org.jpmml.rexp.RVector;

public class RExpUtil {

	private RExpUtil(){
	}

	static
	public byte[] evaluate(Evaluator evaluator, byte[] listBytes) throws Exception {
		RGenericVector argumentsList = (RGenericVector)unserialize(listBytes);

		Map<String, Object> arguments = new LinkedHashMap<>();

		RStringVector argumentNames = argumentsList.names();
		List<RExp> argumentValues = argumentsList.getValues();

		for(int i = 0; i < argumentNames.size(); i++){
			String name = argumentNames.getDequotedValue(i);
			RVector<?> vector = (RVector<?>)argumentValues.get(i);

			Object value = vector.asScalar();

			arguments.put(name, value);
		}

		Map<String, ?> results = evaluator.evaluate(arguments);

		RStringVector resultNames = new RStringVector(new ArrayList<>(results.keySet()), null);
		List<RExp> resultValues = new ArrayList<>();

		for(int i = 0; i < resultNames.size(); i++){
			String name = resultNames.getDequotedValue(i);

			Object value = EvaluatorUtil.decode(results.get(name));

			resultValues.add(createScalar(value));
		}

		RPair attributes = new RPair(new RString("names"), resultNames, null);

		RGenericVector resultsList = new RGenericVector(resultValues, attributes);

		return serialize(resultsList);
	}

	static
	public byte[] evaluateAll(Evaluator evaluator, byte[] dataFrameBytes) throws Exception {
		RGenericVector argumentsDataFrame = (RGenericVector)unserialize(dataFrameBytes);

		Table argumentsTable = parseDataFrame(argumentsDataFrame);

		TableReader argumentsReader = new TableReader(argumentsTable){

			@Override
			public Object get(Object key){
				return super.get((String)key);
			}
		};

		Table resultsTable = new Table();

		TableWriter resultsWriter = new TableWriter(resultsTable){

			@Override
			public Object put(String key, Object value){
				value = EvaluatorUtil.decode(value);

				return super.put(key, value);
			}
		};

		while(argumentsReader.hasNext()){
			Map<String, ?> arguments = argumentsReader.next();

			resultsWriter.next();

			Map<String, ?> results = evaluator.evaluate(arguments);

			resultsWriter.putAll(results);
		}

		resultsTable.canonicalize();

		RGenericVector resultsDataFrame = formatDataFrame(resultsTable);

		return serialize(resultsDataFrame);
	}

	static
	private Table parseDataFrame(RGenericVector dataFrame){
		RStringVector names = dataFrame.names();

		List<String> columns = names.getDequotedValues();

		Table table = new Table(columns);

		for(int i = 0; i < columns.size(); i++){
			String column = columns.get(i);
			RVector<?> vector = dataFrame.getVectorElement(column);

			List<?> values = vector.getValues();

			table.setValues(column, values);
		}

		return table;
	}

	static
	private RGenericVector formatDataFrame(Table table){
		List<String> columns = table.getColumns();

		RStringVector names = new RStringVector(columns, null);

		List<RExp> vectors = new ArrayList<>();

		for(int i = 0; i < columns.size(); i++){
			String column = columns.get(i);
			List<?> values = table.getValues(column);

			RVector<?> vector = createVector(values);

			vectors.add(vector);
		}

		// XXX
		RPair attributes = new RPair(new RString("names"), names, null);

		RGenericVector dataFrame = new RGenericVector(vectors, attributes);

		return dataFrame;
	}

	static
	private RVector<?> createScalar(Object value){

		if(value instanceof Double){
			Double doubleValue = (Double)value;

			return new RDoubleVector(new double[]{doubleValue.doubleValue()}, null);
		} else

		if(value instanceof Float){
			Float floatValue = (Float)value;

			return new RDoubleVector(new double[]{floatValue.floatValue()}, null);
		} else

		if(value instanceof Integer){
			Integer integerValue = (Integer)value;

			return new RIntegerVector(new int[]{integerValue.intValue()}, null);
		} else

		if(value instanceof Boolean){
			Boolean booleanValue = (Boolean)value;

			return new RBooleanVector(new int[]{booleanValue.booleanValue() ? 1 : 0}, null);
		} else

		if(value instanceof String){
			String stringValue = (String)value;

			return new RStringVector(Collections.singletonList(stringValue), null);
		} else

		{
			throw new IllegalArgumentException();
		}
	}

	static
	private RVector<?> createVector(List<?> values){
		Object value = values.get(0);

		if(value instanceof Double){
			return new RDoubleVector(Doubles.toArray((List)values), null);
		} else

		if(value instanceof Float){
			double[] floatValues = new double[values.size()];

			for(int i = 0; i < values.size(); i++){
				floatValues[i] = ((Float)values.get(i)).floatValue();
			}

			return new RDoubleVector(floatValues, null);
		} else

		if(value instanceof Integer){
			return new RIntegerVector(Ints.toArray((List)values), null);
		} else

		if(value instanceof Boolean){
			int[] booleanValues = new int[values.size()];

			for(int i = 0; i < values.size(); i++){
				booleanValues[i] = ((Boolean)values.get(i)).booleanValue() ? 1 : 0;
			}

			return new RBooleanVector(booleanValues, null);
		} else

		if(value instanceof String){
			return new RStringVector((List)values, null);
		} else

		{
			throw new IllegalArgumentException();
		}
	}

	static
	private RExp unserialize(byte[] bytes) throws IOException {

		try(ByteArrayInputStream is = new ByteArrayInputStream(bytes)){

			try(RExpParser parser = new RExpParser(is)){
				return parser.parse();
			}
		}
	}

	static
	private byte[] serialize(RExp rexp) throws IOException {

		try(ByteArrayOutputStream os = new ByteArrayOutputStream()){

			try(RExpWriter writer = new RExpWriter(os)){
				writer.write(rexp);
			}

			return os.toByteArray();
		}
	}
}