package net.zerobuilder.examples.jpa.domain;

import net.zerobuilder.examples.jpa.service.CityService;
import net.zerobuilder.examples.jpa.service.HotelService;
import net.zerobuilder.examples.jpa.service.ReviewRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ReviewTest {

  @Autowired
  CityService cityService;

  @Autowired
  HotelService hotelService;

  @Autowired
  ReviewRepository reviewRepository;

  @Test
  public void test() throws Exception {
    getReviews().forEach(r -> assertThat(r.getTitle(), is(not("FooBar"))));
    getReviews()
        .map(r -> ReviewBuilders.reviewUpdater(r)
            .title("FooBar")
            .done())
        .forEach(reviewRepository::save);
    getReviews().forEach(r -> assertThat(r.getTitle(), is("FooBar")));
  }

  private Page<Review> getReviews() {
    Hotel hotel = hotelService.getHotel(cityService.getCity("Bath", "UK"), "Bath Travelodge");
    return hotelService.getReviews(hotel, new PageRequest(0, 10));
  }
}