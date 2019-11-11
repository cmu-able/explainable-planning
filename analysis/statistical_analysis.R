library(lme4)
library(lmerTest)
# library(r2glmm) # R squared
library(MuMIn) # R squared

# setwd("~/Downloads")
data = read.csv("~/Projects/explainable-planning/analysis/data_3qs.csv")
#data = read.csv("~/Projects/explainable-planning/analysis/data_3qs_aligned.csv")
#data = read.csv("~/Projects/explainable-planning/analysis/data_3qs_unaligned.csv")
names(data)

str(data)
data$accuracy = as.factor(data$accuracy)
table(data$accuracy)

## WOW, look at this!
unique(data$group)
boxplot(list(control = data[data$group=="control",]$score, 
             treatment = data[data$group=="experimental",]$score))

boxplot(list(control = as.numeric(data[data$group=="control",]$accuracy), 
             treatment = as.numeric(data[data$group=="experimental",]$accuracy)))


### ACCURACY ###

# Random intercepts for each participant and each question
m_accuracy = glmer(accuracy ~ 
                     group 
                   + (1|participant) 
                   + (1|question.ref)
                   , family = "binomial"
                   , data = data)

summary(m_accuracy) # experimental group are exp(1.3352) = 3.8 times more likely to answer correctly!

# R2m: describes the proportion of variance explained by the fixed factor(s) alone
# R2c: describes the proportion of variance explained by both the fixed and random factors
r.squaredGLMM(m_accuracy) # goodness of fit of model, without (left) and with (right) random effects

# Doesn't make sense to use ANOVA here since we only have 1 fixed effect
# Instead, report marignal R^2 (R2m) and conditional R^2 (R2c)
# anova(m_accuracy)

# Standard error
se_m_accuracy <- sqrt(diag(vcov(m_accuracy)))

# Table of estimates with 95% CI
tab_ci_m_accuracy <- cbind(Est = fixef(m_accuracy), LL = fixef(m_accuracy) - 1.96 * se_m_accuracy, UL = fixef(m_accuracy) + 1.96 * se_m_accuracy)
# 95% CI [2.04, 7.07]
exp(tab_ci_m_accuracy)

# Random intercepts and slopes for each participant and each question
m_accuracy_pq = glmer(accuracy ~ 
                        group 
                      + (1+group|participant) 
                      + (1+group|question.ref)
                      , family = "binomial"
                      , data = data)

summary(m_accuracy_pq)
r.squaredGLMM(m_accuracy_pq)

# Check if adding random slopes for each participant and each question improves the model fit
# Not very significant: p=0.07
anova(m_accuracy, m_accuracy_pq, refit=FALSE)

# Random slope for each participant, but not for each question
m_accuracy_p = glmer(accuracy ~ 
                       group 
                     + (1+group|participant) 
                     + (1|question.ref)
                     , family = "binomial"
                     , data = data)

summary(m_accuracy_p)
r.squaredGLMM(m_accuracy_p)

# Check if adding random slope for each participant (but not each question) improves the model fit
# Not significant: p=0.69
anova(m_accuracy, m_accuracy_p, refit=FALSE)

# Random slope for each question, but not for each participant
m_accuracy_q = glmer(accuracy ~ 
                       group 
                     + (1|participant) 
                     + (1+group|question.ref)
                     , family = "binomial"
                     , data = data)

summary(m_accuracy_q)
r.squaredGLMM(m_accuracy_q)

# Check if adding random slope for each question (but not each participant) improves the model fit
# Somewhat significant: p=0.019
anova(m_accuracy, m_accuracy_q, refit=FALSE)

# Comparing models based on BIC
# The best model according to BIC is m_accuracy
BIC(m_accuracy, m_accuracy_pq, m_accuracy_p, m_accuracy_q)


### CONFIDENCE-WEIGHTED SCORE ###
table(data$score)

# Random intercepts for each participant and each question
m_score = lmer(score ~ 
                 group 
               + (1|participant) 
               + (1|question.ref)
               , data = data)

summary(m_score)
r.squaredGLMM(m_score)

# Doesn't make sense to use ANOVA here since we only have 1 fixed effect
# Instead, report marignal R^2 (R2m) and conditional R^2 (R2c)
# anova(m_score)

# Standard error
se_m_score <- sqrt(diag(vcov(m_score)))

# Table of estimates with 95% CI
tab_ci_m_score <- cbind(Est = fixef(m_score), LL = fixef(m_score) - 1.96 * se_m_score, UL = fixef(m_score) + 1.96 * se_m_score)
# 95% CI [1.04, 2.42]
tab_ci_m_score

# Random slopes for each participant and each question
m_score_pq = lmer(score ~ 
                    group 
                  + (1+group|participant) 
                  + (1+group|question.ref)
                  , data = data)

summary(m_score_pq)
r.squaredGLMM(m_score_pq)

# Check if adding random slopes for each participant and each question improves the model fit
# Somewhat significant: p=0.013
anova(m_score, m_score_pq, refit=FALSE)

# Random slope for each participant, but not for each question
m_score_p = lmer(score ~ 
                   group 
                 + (1+group|participant) 
                 + (1|question.ref)
                 , data = data)

# Model failed to converge
summary(m_score_p)
r.squaredGLMM(m_score_p)
anova(m_score, m_score_p, refit=FALSE)

# Random slope for each question, but not for each participant
m_score_q = lmer(score ~ 
                   group 
                 + (1|participant) 
                 + (1+group|question.ref)
                 , data = data)

summary(m_score_q)
r.squaredGLMM(m_score_q)

# Check if adding random slope for each question (but not each paricipant) improves the model fit
# Somewhat significant: p=0.010
anova(m_score, m_score_q, refit=FALSE)

# Comparing models based on BIC
# The best model according to BIC is m_score
BIC(m_score, m_score_pq, m_score_p, m_score_q)