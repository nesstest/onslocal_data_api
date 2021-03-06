package gov.ons.local.data.resources;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import gov.ons.local.data.DataDTO;
import gov.ons.local.data.VariableDTO;
import gov.ons.local.data.entity.Category;
import gov.ons.local.data.entity.DataResource;
import gov.ons.local.data.entity.DimensionalDataSet;
import gov.ons.local.data.entity.GeographicArea;
import gov.ons.local.data.entity.GeographicLevelType;
import gov.ons.local.data.entity.Presentation;
import gov.ons.local.data.entity.Taxonomy;
import gov.ons.local.data.entity.Variable;
import gov.ons.local.data.session.category.CategoryFacade;
import gov.ons.local.data.session.data.DataFacade;
import gov.ons.local.data.session.dataresource.DataResourceFacade;
import gov.ons.local.data.session.dimensionaldataset.DimensionalDataSetFacade;
import gov.ons.local.data.session.geographicarea.GeographicAreaFacade;
import gov.ons.local.data.session.geographicleveltype.GeographicLevelTypeFacade;
import gov.ons.local.data.session.presentation.PresentationFacade;
import gov.ons.local.data.session.time.TimeFacade;
import gov.ons.local.data.session.variable.VariableFacade;

@Path("local-data")
public class Resource
{

	private Logger logger = Logger.getLogger(Resource.class.getSimpleName());

	@Inject
	private GeographicLevelTypeFacade geographicLevelTypeFacade;

	@Inject
	private DataResourceFacade dataResourceFacade;

	@Inject
	private VariableFacade variableFacade;

	@Inject
	private GeographicAreaFacade geographicAreaFacade;

	@Inject
	private DataFacade dataFacade;

	@Inject
	private CategoryFacade categoryFacade;

	@Inject
	private TimeFacade timeFacade;

	@Inject
	private PresentationFacade presentationFacade;
	
	@Inject
	private DimensionalDataSetFacade dimensionalDataSetFacade;

	@GET
	@Path("/keywordsearch")
	@Produces({ MediaType.APPLICATION_JSON })
	public String keyWordSearch(@QueryParam("searchTerm") String searchTerm)
	{
		// e.g.
		// http://localhost:8080/local-data-web/rs/local-data/keywordsearch?searchTerm=house
		// http://ec2-52-25-128-99.us-west-2.compute.amazonaws.com/local-data-web/rs/local-data/keywordsearch?searchTerm=house

		if (searchTerm == null || searchTerm.length() == 0)

			logger.log(Level.INFO, "keyWordSearch: searchTerm = " + searchTerm);

		List<DataResource> results = dataResourceFacade
				.searchKeyWord("%" + searchTerm.toLowerCase().trim() + "%");

		JsonArrayBuilder arrBuilder = Json.createArrayBuilder();
		JsonArrayBuilder taxArrBuilder = Json.createArrayBuilder();

		for (DataResource dr : results)
		{
			for (Taxonomy t : dr.getTaxonomies())
			{
				taxArrBuilder.add(t.getTaxonomy());
			}

			arrBuilder.add(Json.createObjectBuilder()
					.add("data_resource", dr.getDataResource())
					.add("title", dr.getTitle())
					.add("metadata",
							dr.getMetadata() != null ? dr.getMetadata() : "")
					.add("taxonomies", taxArrBuilder.build()));
		}

		JsonObject output = Json.createObjectBuilder()
				.add("data_resources", arrBuilder.build()).build();

		return output.toString();
	}

	@GET
	@Path("/dataresource")
	@Produces({ MediaType.APPLICATION_JSON })
	public String findByDataResource(
			@QueryParam("dataResource") String dataResource)
	{
		// e.g.
		// http://localhost:8080/local-data-web/rs/local-data/dataresource?dataResource=G39
		// http://ec2-52-25-128-99.us-west-2.compute.amazonaws.com/local-data-web/rs/local-data/dataresource?dataResource=G39

		if (dataResource != null && dataResource.length() > 0)
		{
			// Find the DataResource
			DataResource dr = dataResourceFacade.findById(dataResource);

			if (dr != null)
			{
				logger.log(Level.INFO,
						"findByDataResource: dataResource = " + dataResource);

				List<Variable> results = variableFacade.findByDataResource(dr);

				JsonArrayBuilder arrBuilder = Json.createArrayBuilder();

				for (Variable v : results)
				{
					arrBuilder.add(Json.createObjectBuilder()
							.add("variable_id", v.getVariableId())
							.add("name", v.getName())
							.add("value_domain",
									v.getValueDomainBean().getValueDomain())
							.add("unit_type", v.getUnitTypeBean().getUnitType()));
				}

				JsonObject output = Json.createObjectBuilder()
						.add("variables", arrBuilder.build()).build();

				return output.toString();
			}
		}

		return Json.createObjectBuilder().add("error", "no-data").build()
				.toString();
	}

	@GET
	@Path("/extcode")
	@Produces({ MediaType.APPLICATION_JSON })
	public String findByExtCodeLevel(
			@QueryParam("dataResource") String dataResource,
			@QueryParam("extCode") String extCode,
			@QueryParam("geographicLevelType") String geographicLevelType)
	{
		// e.g.
		// http://localhost:8080/local-data-web/rs/local-data/data?dataResource=G39&extCode=E07000087&geographicLevelType=LA&timePeriod=19
		// http://ec2-52-25-128-99.us-west-2.compute.amazonaws.com/local-data-web/rs/local-data/extcode?dataResource=G39&extCode=E07000087&geographicLevelType=LA

		if ((dataResource != null && dataResource.length() > 0)
				&& (extCode != null && extCode.length() > 0)
				&& (geographicLevelType != null
						&& geographicLevelType.length() > 0))
		{
			// Find the DataResource
			DataResource dr = dataResourceFacade.findById(dataResource);

			// Find the GeographicLevelType
			GeographicLevelType glt = geographicLevelTypeFacade
					.findById(geographicLevelType);

			if (dr != null && glt != null)
			{
				GeographicArea result = geographicAreaFacade.findByExtCodeLevel(dr,
						glt, extCode);

				if (result != null)
				{
					JsonObject output = Json.createObjectBuilder()
							.add("geographic_area_id", result.getGeographicAreaId())
							.add("ext_code", result.getExtCode())
							.add("name", result.getName())
							.add("geographic_level_type",
									result.getGeographicLevelTypeBean()
											.getGeographicLevelType())
							.build();

					return output.toString();
				}

			}
		}

		return Json.createObjectBuilder().add("error", "no-data").build()
				.toString();
	}

	@GET
	@Path("/data")
	@Produces({ MediaType.APPLICATION_JSON })
	public String findByExtCodeLevel(
			@QueryParam("dataResource") String dataResource,
			@QueryParam("extCode") String extCode,
			@QueryParam("geographicLevelType") String geographicLevelType,
			@QueryParam("timePeriod") String timePeriod)
	{
		// e.g.
		// http://localhost:8080/local-data-web/rs/local-data/data?dataResource=G39&extCode=E07000087&geographicLevelType=LA&timePeriod=19
		// http://ec2-52-25-128-99.us-west-2.compute.amazonaws.com/local-data-web/rs/local-data/data?dataResource=G39&extCode=E07000087&geographicLevelType=LA&timePeriod=19

		// Check input parameters
		if ((dataResource != null && dataResource.length() > 0)
				&& (extCode != null && extCode.length() > 0)
				&& (geographicLevelType != null && geographicLevelType.length() > 0)
				&& (timePeriod != null && timePeriod.length() > 0))
		{
			List<DataDTO> results = dataFacade.getVariableValues(extCode,
					geographicLevelType, timePeriod, dataResource);

			if (results != null && results.size() > 0)
			{
				Map<Long, List<String>> varConceptSysMap = new HashMap<>();

				// Add the variable ids to the Map
				for (DataDTO d : results)
				{
					varConceptSysMap.put(d.getVariableId(), new ArrayList<String>());
				}

				// Get the conceptSystems related to these variables
				List<VariableDTO> variables = variableFacade
						.findByIds(varConceptSysMap.keySet());

				for (VariableDTO v : variables)
				{
					varConceptSysMap.put(v.getId(), v.getConceptSystems());
				}

				JsonArrayBuilder arrBuilder = Json.createArrayBuilder();
				JsonArrayBuilder conSysArrBuilder = Json.createArrayBuilder();

				// Add results to Map to remove duplicates that are caused by
				// multiple hierarchies
				Map<Long, DataDTO> resultMap = new HashMap<>();

				for (DataDTO d : results)
				{
					resultMap.put(d.getVariableId(), d);
				}

				for (DataDTO d : resultMap.values())
				{
					String value = d.getValue() != null ? d.getValue().toString()
							: "";

					for (String s : varConceptSysMap.get(d.getVariableId()))
					{
						conSysArrBuilder.add(s);
					}

					arrBuilder.add(Json.createObjectBuilder()
							.add("variable_id", d.getVariableId())
							.add("name", d.getName())
							.add("value_domain", d.getValueDomain())
							.add("unit_type", d.getUnitType())
							.add("variable_name", d.getVariableName())
							.add("value", value)
							.add("concept_systems", conSysArrBuilder.build()));
				}

				JsonObject output = Json.createObjectBuilder()
						.add("ext_code", results.get(0).getExtCode())
						.add("geographic_area_id",
								results.get(0).getGeographicAreaId())
						.add("variables", arrBuilder.build()).build();

				return output.toString();
			}
		}

		return Json.createObjectBuilder().add("error", "no-data").build()
				.toString();
	}

	@GET
	@Path("/geolevels")
	@Produces({ MediaType.APPLICATION_JSON })
	public String findGeoAreaByDataResource(
			@QueryParam("dataResource") String dataResource)
	{
		// e.g.
		// http://localhost:8080/local-data-web/rs/local-data/geolevels?dataResource=G39
		// http://ec2-52-25-128-99.us-west-2.compute.amazonaws.com/local-data-web/rs/local-data/geolevels?dataResource=G39

		if (dataResource != null && dataResource.length() > 0)
		{
			// Find the DataResource
			DataResource dr = dataResourceFacade.findById(dataResource);

			if (dr != null)
			{
				Set<String> results = geographicAreaFacade.findByDataResource(dr);

				List<GeographicLevelType> levelTypes = geographicLevelTypeFacade
						.findByIds(results);

				JsonArrayBuilder arrBuilder = Json.createArrayBuilder();

				for (GeographicLevelType glt : levelTypes)
				{
					arrBuilder.add(Json.createObjectBuilder()
							.add("geographic_level_type", glt.getGeographicLevelType())
							.add("metadata", glt.getMetadata())
							.add("layers", glt.getLayers()));
				}

				JsonObject output = Json.createObjectBuilder()
						.add("geographic_level_types", arrBuilder.build()).build();

				return output.toString();
			}
		}

		return Json.createObjectBuilder().add("error", "no-data").build()
				.toString();
	}
	
	

	@GET
	@Path("/getconceptsystem")
	@Produces({ MediaType.APPLICATION_JSON })
	public String getConceptSytemByDataResource(
			@QueryParam("dataResource") String dataResource)
	{
		// e.g.
		// http://localhost:8080/local-data-web/rs/local-data/getconceptsystem?dataResource=G39
		// http://ec2-52-25-128-99.us-west-2.compute.amazonaws.com/local-data-web/rs/local-data/getconceptsystem?dataResource=G39

		if (dataResource != null && dataResource.length() > 0)
		{
			// Find the DataResource
			DataResource dr = dataResourceFacade.findById(dataResource);

			if (dr != null)
			{
				// Get the variables associated with this data resource
				List<Variable> variables = variableFacade.findByDataResource(dr);

				// Get categories associated with these variables
				Collection<Category> categories = variableFacade
						.getConceptSystemByVariables(variables);

				JsonArrayBuilder arrBuilder = Json.createArrayBuilder();

				for (Category c : categories)
				{
					arrBuilder.add(Json.createObjectBuilder()
							.add("name", c.getName()).add("concept_system",
									c.getConceptSystemBean().getConceptSystem()));
				}
  
				    JsonArray jsonArr = arrBuilder.build();
				    JsonArrayBuilder sortedJsonArray = Json.createArrayBuilder();

				    List<JsonObject> jsonValues = new ArrayList<JsonObject>();
				    for (int i = 0; i < jsonArr.size(); i++) {
				        jsonValues.add(jsonArr.getJsonObject(i));
				    }
				    Collections.sort( jsonValues, new Comparator<JsonObject>() {
				        //You can change "Name" with "ID" if you want to sort by ID
				        private static final String KEY_NAME = "concept_system";

				        @Override
				        public int compare(JsonObject a, JsonObject b) {
				            String valA = new String();
				            String valB = new String();

				            try {
				                valA = (String) a.get(KEY_NAME).toString();
				                valB = (String) b.get(KEY_NAME).toString();
				            } 
				            catch (Exception e) {
				                //do something
				            }

				            return valA.compareTo(valB);
				            //if you want to change the sort order, simply use the following:
				            //return -valA.compareTo(valB);
				        }
				    });

				    for (int i = 0; i < jsonArr.size(); i++) {
				        sortedJsonArray.add(jsonValues.get(i));
				    }
				
			//	JsonObject output = Json.createObjectBuilder()
			//			.add("concept_systems", arrBuilder.build()).build();

				JsonObject output = Json.createObjectBuilder()
								.add("concept_systems", sortedJsonArray.build()).build();
				    
				return output.toString();
			}
		}

		return Json.createObjectBuilder().add("error", "no-data").build()
				.toString();
	}

	@GET
	@Path("/latesttime")
	@Produces({ MediaType.APPLICATION_JSON })
	public String findLatestTimeByDataResource(
			@QueryParam("dataResource") String dataResource)
	{
		// e.g.
		// http://localhost:8080/local-data-web/rs/local-data/latesttime?dataResource=G39
		// http://ec2-52-25-128-99.us-west-2.compute.amazonaws.com/local-data-web/rs/local-data/latesttime?dataResource=G39

		if (dataResource != null && dataResource.length() > 0)
		{
			// Find the DataResource
			DataResource dr = dataResourceFacade.findById(dataResource);

			if (dr != null)
			{
				BigInteger result = timeFacade.findLatestTimeByDataResource(dr);

				if (result != null)
				{
					JsonObject output = Json.createObjectBuilder()
							.add("time_period_id", result).build();

					return output.toString();
				}
			}
		}

		return Json.createObjectBuilder().add("error", "no-data").build()
				.toString();
	}

	@GET
	@Path("/presentation")
	@Produces({ MediaType.APPLICATION_JSON })
	public String findPresentationByDataResource(
			@QueryParam("dataResource") String dataResource)
	{
		// e.g.
		// http://localhost:8080/local-data-web/rs/local-data/presentation?dataResource=G39
		// http://ec2-52-25-128-99.us-west-2.compute.amazonaws.com/local-data-web/rs/local-data/presentation?dataResource=G39

		if (dataResource != null && dataResource.length() > 0)
		{
			// Find the DataResource
			DataResource dr = dataResourceFacade.findById(dataResource);

			if (dr != null)
			{
				logger.log(Level.INFO,
						"findByDataResource: dataResource = " + dataResource);

				List<Presentation> results = presentationFacade
						.findByDataResource(dr);

				JsonArrayBuilder arrBuilder = Json.createArrayBuilder();

				for (Presentation p : results)
				{
					arrBuilder
							.add(Json.createObjectBuilder()
									.add("dimensional_data_set_id",
											p.getDimensionalDataSet()
													.getDimensionalDataSetId())
									.add("download_url", p.getDownloadurl()));
				}

				JsonObject output = Json.createObjectBuilder()
						.add("presentations", arrBuilder.build()).build();

				return output.toString();
			}
		}

		return Json.createObjectBuilder().add("error", "no-data").build()
				.toString();
	}

	@GET
	@Path("/dataresourcetitle")
	@Produces({ MediaType.APPLICATION_JSON })
	public String getTitleByDataResource(
			@QueryParam("dataResource") String dataResource)

	{
		// e.g.
		// http://localhost:8080/local-data-web/rs/local-data/dataresourcetitle?dataResource=G39
		// http://ec2-52-25-128-99.us-west-2.compute.amazonaws.com/local-data-web/rs/local-data/dataresourcetitle?dataResource=G39

		if (dataResource != null && dataResource.length() > 0)
		{
			// Find the DataResource
			DataResource dr = dataResourceFacade.findById(dataResource);

			if (dr != null)
			{
				JsonObject output = Json.createObjectBuilder()
						.add("title", dr.getTitle()).build();

				return output.toString();
			}
		}

		return Json.createObjectBuilder().add("error", "no-data").build()
				.toString();
	}
	
	@GET
	@Path("/metadata")
	@Produces({ MediaType.APPLICATION_JSON })
	public String getMetadataDataResource(
			@QueryParam("dataResource") String dataResource)

	{
		// e.g.
		// http://localhost:8080/local-data-web/rs/local-data/metadata?dataResource=G39
		// http://ec2-52-25-128-99.us-west-2.compute.amazonaws.com/local-data-web/rs/local-data/metadata?dataResource=G39

		if (dataResource != null && dataResource.length() > 0)
		{
			// Find the DataResource
			DataResource dr = dataResourceFacade.findById(dataResource);

			if (dr != null)
			{
				BigInteger timePeriodId = timeFacade.findLatestTimeByDataResource(dr);
				
				if (timePeriodId != null)
				{
					DimensionalDataSet result = dimensionalDataSetFacade.findLatestByDataResource(dr, timePeriodId);
					
					JsonObject output = Json.createObjectBuilder()
							.add("dimensional_data_set_id",
									result.getDimensionalDataSetId())
							.add("title", result.getTitle())
							.add("metadata", result.getMetadata())
							.add("source", result.getSource())
							.add("contact", result.getContact())
							.add("release_date", result.getReleaseDate())
							.add("next_release", result.getNextRelease()).build();
					
					return output.toString();
				}			
			}
		}

		return Json.createObjectBuilder().add("error", "no-data").build()
				.toString();
	}
}
