package com.example.goalbot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.goalbot.common.AdviceSourceHasher;
import com.example.goalbot.entity.Review;
import com.example.goalbot.mapper.ReviewMapper;
import com.example.goalbot.service.ReviewService;
import com.example.goalbot.vo.ReviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl extends ServiceImpl<ReviewMapper, Review> implements ReviewService {

    @Override
    @Transactional
    public ReviewVO saveOrUpdateReview(Long userId, LocalDate reviewDate, Integer type, String summary, String aiAdvice) {
        Review review = getOne(new LambdaQueryWrapper<Review>()
                .eq(Review::getUserId, userId)
                .eq(Review::getReviewDate, reviewDate)
                .eq(Review::getType, type));
        if (review == null) {
            review = new Review();
            review.setUserId(userId);
            review.setReviewDate(reviewDate);
            review.setType(type);
            review.setSummary(summary);
            review.setAiAdvice(aiAdvice);
            save(review);
        } else {
            review.setSummary(summary);
            review.setAiAdvice(aiAdvice);
            updateById(review);
        }
        return toVO(review);
    }

    @Override
    public List<ReviewVO> listReviews(Long userId, Integer type, LocalDate startDate, LocalDate endDate) {
        return list(new LambdaQueryWrapper<Review>()
                .eq(Review::getUserId, userId)
                .eq(type != null, Review::getType, type)
                .ge(startDate != null, Review::getReviewDate, startDate)
                .le(endDate != null, Review::getReviewDate, endDate)
                .orderByDesc(Review::getReviewDate)
                .orderByDesc(Review::getCreatedAt))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public ReviewVO getLatestReview(Long userId, Integer type) {
        Review review = getOne(new LambdaQueryWrapper<Review>()
                .eq(Review::getUserId, userId)
                .eq(type != null, Review::getType, type)
                .orderByDesc(Review::getReviewDate)
                .orderByDesc(Review::getCreatedAt)
                .last("LIMIT 1"));
        return review == null ? null : toVO(review);
    }

    private ReviewVO toVO(Review review) {
        ReviewVO vo = new ReviewVO();
        BeanUtils.copyProperties(review, vo);
        vo.setSummary(AdviceSourceHasher.displaySummary(review.getSummary()));
        return vo;
    }
}
