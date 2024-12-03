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

import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.EvaluatorUtil;
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
			RVector<?> vector;

			Object value = EvaluatorUtil.decode(results.get(name));

			if(value instanceof Double){
				Double doubleValue = (Double)value;

				vector = new RDoubleVector(new double[]{doubleValue.doubleValue()}, null);
			} else

			if(value instanceof Float){
				Float floatValue = (Float)value;

				vector = new RDoubleVector(new double[]{floatValue.floatValue()}, null);
			} else

			if(value instanceof Integer){
				Integer integerValue = (Integer)value;

				vector = new RIntegerVector(new int[]{integerValue.intValue()}, null);
			} else

			if(value instanceof Boolean){
				Boolean booleanValue = (Boolean)value;

				vector = new RBooleanVector(new int[]{booleanValue.booleanValue() ? 1 : 0}, null);
			} else

			if(value instanceof String){
				String stringValue = (String)value;

				vector = new RStringVector(Collections.singletonList(stringValue), null);
			} else

			{
				throw new IllegalArgumentException();
			}

			resultValues.add(vector);
		}

		RPair attributes = new RPair(new RString("names"), resultNames, null);

		RGenericVector resultsList = new RGenericVector(resultValues, attributes);

		return serialize(resultsList);
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