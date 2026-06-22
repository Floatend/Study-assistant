package com.example.goalbot.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.goalbot.entity.Review;
import com.example.goalbot.vo.ReviewVO;

import java.time.LocalDate;
import java.util.List;

public interface ReviewService extends IService<Review> {

    ReviewVO saveOrUpdateReview(Long userId, LocalDate reviewDate, Integer type, String summary, String aiAdvice);

    List<ReviewVO> listReviews(Long userId, Integer type, LocalDate startDate, LocalDate endDate);

    ReviewVO getLatestReview(Long userId, Integer type);
}
