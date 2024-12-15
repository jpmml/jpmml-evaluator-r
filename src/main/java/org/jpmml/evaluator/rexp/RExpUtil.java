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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.jpmml.rexp.RStringVector;
import org.jpmml.rexp.RVector;

public class RExpUtil {

	private RExpUtil(){
	}

	static
	public byte[] evaluate(Evaluator evaluator, byte[] listBytes) throws Exception {
		RGenericVector argumentsList = (RGenericVector)unserialize(listBytes);

		RGenericVector resultsList = evaluate(evaluator, argumentsList);

		return serialize(resultsList);
	}

	static
	public RGenericVector evaluate(Evaluator evaluator, RGenericVector argumentsList){
		Map<String, ?> arguments = parseNamedList(argumentsList);

		Map<String, ?> results = evaluator.evaluate(arguments);

		results = EvaluatorUtil.decodeAll(results);

		return formatNamedList(results);
	}

	static
	public byte[] evaluateAll(Evaluator evaluator, byte[] dataFrameBytes) throws Exception {
		RGenericVector argumentsDataFrame = (RGenericVector)unserialize(dataFrameBytes);

		RGenericVector resultsDataFrame = evaluateAll(evaluator, argumentsDataFrame);

		return serialize(resultsDataFrame);
	}

	static
	public RGenericVector evaluateAll(Evaluator evaluator, RGenericVector argumentsDataFrame){
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

		return formatDataFrame(resultsTable);
	}

	static
	private Map<String, ?> parseNamedList(RGenericVector namedList){
		RStringVector names = namedList.names();

		Map<String, Object> result = new LinkedHashMap<>();

		for(int i = 0; i < names.size(); i++){
			String name = names.getDequotedValue(i);
			RVector<?> vector = (RVector<?>)namedList.getValue(i);

			Object value = vector.asScalar();

			result.put(name, value);
		}

		return result;
	}

	static
	private RGenericVector formatNamedList(Map<String, ?> map){
		List<String> names = new ArrayList<>();
		List<RExp> values = new ArrayList<>();

		Set<? extends Map.Entry<String, ?>> entries = map.entrySet();
		for(Map.Entry<String, ?> entry : entries){
			String name = entry.getKey();
			Object value = entry.getValue();

			RVector<?> vector = createScalar(value);

			names.add(name);
			values.add(vector);
		}

		RGenericVector result = new RGenericVector(values, null);
		result.addAttribute("names", new RStringVector(names, null));

		return result;
	}

	static
	private Table parseDataFrame(RGenericVector dataFrame){
		RStringVector names = dataFrame.names();

		Table result = new Table(names.getDequotedValues());

		for(int i = 0; i < names.size(); i++){
			String name = names.getDequotedValue(i);
			RVector<?> vector = (RVector<?>)dataFrame.getValue(i);

			List<?> values = vector.getValues();

			result.setValues(name, values);
		}

		return result;
	}

	static
	private RGenericVector formatDataFrame(Table table){
		List<String> names = new ArrayList<>(table.getColumns());
		List<RExp> vectors = new ArrayList<>();

		for(String name : names){
			List<?> values = table.getValues(name);

			RVector<?> vector = createVector(values);

			vectors.add(vector);
		}

		RGenericVector result = new RGenericVector(vectors, null);
		result.addAttribute("names", new RStringVector(names, null));

		return result;
	}

	static
	private RVector<?> createScalar(Object value){

		if(value instanceof Double || value instanceof Float){
			return new RDoubleVector((Number)value, null);
		} else

		if(value instanceof Integer){
			return new RIntegerVector((Integer)value, null);
		} else

		if(value instanceof Boolean){
			return new RBooleanVector((Boolean)value, null);
		} else

		if(value instanceof String){
			return new RStringVector((String)value, null);
		} else

		{
			throw new IllegalArgumentException();
		}
	}

	static
	private RVector<?> createVector(List<?> values){
		Object value = (values.iterator()).next();

		if(value instanceof Double || value instanceof Float){
			return new RDoubleVector((List)values, null);
		} else

		if(value instanceof Integer){
			return new RIntegerVector((List)values, null);
		} else

		if(value instanceof Boolean){
			return new RBooleanVector((List)values, null);
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