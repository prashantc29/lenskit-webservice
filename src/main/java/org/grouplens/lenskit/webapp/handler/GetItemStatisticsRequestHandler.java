package org.grouplens.lenskit.webapp.handler;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.grouplens.common.dto.DtoContainer;
import org.grouplens.lenskit.webapp.BadRequestException;
import org.grouplens.lenskit.webapp.ServerUtils;
import org.grouplens.lenskit.webapp.ServerUtils.ParsedUrl;
import org.grouplens.lenskit.webapp.ServerUtils.SerializationFormat;
import org.grouplens.lenskit.webapp.Session;
import org.grouplens.lenskit.webapp.dto.ItemStatisticsDto;

//Invoked by calling GET /items/[iid]/statistics
public class GetItemStatisticsRequestHandler extends RequestHandler {

	public GetItemStatisticsRequestHandler() {
		super();
		addResource("items", true);
		addResource("statistics", false);
	}

	@Override
	public RequestMethod getMethod() {
		return RequestMethod.GET;
	}

	@Override
	public void handle(Session session, ParsedUrl parsed, HttpServletRequest request, HttpServletResponse response) throws Exception {
		try {
			long iid = Long.parseLong(parsed.getResourceMap().get("items"));
			SerializationFormat responseFormat = ServerUtils.determineResponseFormat(parsed, request.getHeader("Accept"));
			int eventCount = session.getItemEvents(iid).size();
			Map<Long, Double> latestRatings = session.getCurrentItemRatings(iid);
			double ratingTotal = 0;
			int nRatings = 0;
			for (Double ratingVal : latestRatings.values()) {
				ratingTotal += ratingVal;
				nRatings++;
			}
			double averageRating = 0;
			if (nRatings != 0) averageRating = ratingTotal/nRatings;
			ItemStatisticsDto dto = new ItemStatisticsDto(Long.toString(iid), eventCount, nRatings, averageRating);
			DtoContainer<ItemStatisticsDto> container = new DtoContainer<ItemStatisticsDto>(ItemStatisticsDto.class, dto);
			writeResponse(container, response, responseFormat);
		} catch (NumberFormatException e) {
			throw new BadRequestException("Invalid Item ID", e);
		}
	}
}