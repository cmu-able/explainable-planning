library(lme4)
library(lmerTest) # Comment this out when using stargazer
# library(r2glmm) # R squared
library(MuMIn) # R squared
library(stargazer)

# setwd("~/Downloads")
setwd("~/Projects/explainable-planning/analysis/data")
data_all = read.csv("data_3qs.csv")
data_aligned = read.csv("data_3qs_aligned.csv")
data_unaligned = read.csv("data_3qs_unaligned.csv")

data = data_all
# data = data_aligned
# data = data_unaligned
names(data)

str(data)
data$accuracy = as.factor(data$accuracy)
table(data$accuracy)

## WOW, look at this!
unique(data$group)

# Confidence-weighted score
boxplot(list(control = data[data$group=="control",]$score, 
             treatment = data[data$group=="experimental",]$score))

# Confidence level
boxplot(list(control = data[data$group=="control",]$confidence,
             treatment = data[data$group=="experimental",]$confidence))

# Correctness
boxplot(list(control = as.numeric(data[data$group=="control",]$accuracy), 
             treatment = as.numeric(data[data$group=="experimental",]$accuracy)))


# Reference: https://web.stanford.edu/class/psych252/section/Mixed_models_tutorial.html
# Read: REML vs. ML

### ACCURACY ###

## MODEL 1
# Random intercepts for each participant and each question
m_accuracy = glmer(accuracy ~ 
                     group 
                   + case
                   + (1|participant) 
                   + (1|question.ref)
                   , family = "binomial"
                   , data = data)

summary(m_accuracy) # experimental group are exp(1.3366) = 3.8 times more likely to answer correctly!

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

# 95% CI of group: [2.03, 7.12]
# 95% CI of case: [0.19, 0.70]
exp(tab_ci_m_accuracy)

# MODEL 1.1
# Check if there is an interaction between "group" and "case" on accuracy
m_accuracy_gc = glmer(accuracy ~ 
                     group 
                   * case
                   + (1|participant) 
                   + (1|question.ref)
                   , family = "binomial"
                   , data = data)

summary(m_accuracy_gc)

# Check if adding group*case term improves the model fit
# Not significant: p=0.06224
anova(m_accuracy, m_accuracy_gc, refit=FALSE)

## MODEL 2
# Random slopes for each participant and each question -- in addition to random intercepts
# Model failed to converge
m_accuracy_pq = glmer(accuracy ~ 
                        group 
                      + case
                      + (1+group|participant) 
                      + (1+group|question.ref)
                      , family = "binomial"
                      , data = data)

summary(m_accuracy_pq)
r.squaredGLMM(m_accuracy_pq)

# Check if adding random slopes for each participant and each question improves the model fit
# Not conclusive because model failed to converge
anova(m_accuracy, m_accuracy_pq, refit=FALSE)

## MODEL 3
# Random slope for each participant, but not for each question -- in addition to random intercepts
# Model failed to converge
m_accuracy_p = glmer(accuracy ~ 
                       group 
                     + case
                     + (1+group|participant) 
                     + (1|question.ref)
                     , family = "binomial"
                     , data = data)

summary(m_accuracy_p)
r.squaredGLMM(m_accuracy_p)

# Check if adding random slope for each participant (but not each question) improves the model fit
# Not conclusive because model failed to converge
anova(m_accuracy, m_accuracy_p, refit=FALSE)

## MODEL 4
# Random slope for each question, but not for each participant -- in addition to random intercepts
m_accuracy_q = glmer(accuracy ~ 
                       group 
                     + case
                     + (1|participant) 
                     + (1+group|question.ref)
                     , family = "binomial"
                     , data = data)

summary(m_accuracy_q)
r.squaredGLMM(m_accuracy_q)

# Check if adding random slope for each question (but not each participant) improves the model fit
# Significant: p=0.004847
anova(m_accuracy, m_accuracy_q, refit=FALSE)

# Comparing models based on BIC
# The best model according to BIC is m_accuracy
BIC(m_accuracy, m_accuracy_pq, m_accuracy_p, m_accuracy_q)


### CONFIDENCE-WEIGHTED SCORE ###
table(data$score)

## MODEL 1
# Random intercepts for each participant and each question
m_score = lmer(score ~ 
                 group 
               + case
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

# 95% CI of group: [1.03, 2.42]
# 95% CI of case: [-1.81, -0.38]
tab_ci_m_score

# MODEL 1.1
# Check if there is an interaction between "group" and "case" on score
m_score_gc = lmer(score ~ 
                    group 
                  * case 
                  + (1|participant) 
                  + (1|question.ref)
                  , data = data)

summary(m_score_gc)

# Check if adding group*case term improves the model fit
# Significant: p=0.002486
anova(m_score, m_score_gc, refit=FALSE)

## MODEL 2
# Random slopes for each participant and each question -- in addition to random intercepts
# Model failed to converge
m_score_pq = lmer(score ~ 
                    group 
                  + case
                  + (1+group|participant) 
                  + (1+group|question.ref)
                  , data = data)

summary(m_score_pq)
r.squaredGLMM(m_score_pq)

# Check if adding random slopes for each participant and each question improves the model fit
# Not conclusive because model failed to converge
anova(m_score, m_score_pq, refit=FALSE)

## MODEL 3
# Random slope for each participant, but not for each question -- in addition to random intercepts
# Model failed to converge
m_score_p = lmer(score ~ 
                   group 
                 + case
                 + (1+group|participant) 
                 + (1|question.ref)
                 , data = data)

summary(m_score_p)
r.squaredGLMM(m_score_p)

# Check if adding random slope for each participant (but not each question) improves the model fit
# Not conclusive because model failed to converge
anova(m_score, m_score_p, refit=FALSE)

## MODEL 4
# Random slope for each question, but not for each participant -- in addition to random intercepts
m_score_q = lmer(score ~ 
                   group 
                 + case
                 + (1|participant) 
                 + (1+group|question.ref)
                 , data = data)

summary(m_score_q)
r.squaredGLMM(m_score_q)

# Check if adding random slope for each question (but not each participant) improves the model fit
# Significant: p=0.004546 
anova(m_score, m_score_q, refit=FALSE)

# Comparing models based on BIC
# The best model according to BIC is m_score
BIC(m_score, m_score_pq, m_score_p, m_score_q)


### CONFIDENCE LEVEL ###
table(data$confidence)

## MODEL 1
# Random intercepts for each participant and each question
m_confidence = lmer(confidence ~ 
                      group 
                    + case 
                    + (1|participant) 
                    + (1|question.ref)
                    , data = data)

summary(m_confidence)
r.squaredGLMM(m_confidence)

# Doesn't make sense to use ANOVA here since we only have 1 fixed effect
# Instead, report marignal R^2 (R2m) and conditional R^2 (R2c)
# anova(m_confidence)

# Standard error
se_m_confidence <- sqrt(diag(vcov(m_confidence)))

# Table of estimates with 95% CI
tab_ci_m_confidence <- cbind(Est = fixef(m_confidence), LL = fixef(m_confidence) - 1.96 * se_m_confidence, UL = fixef(m_confidence) + 1.96 * se_m_confidence)

# 95% CI of group: [0.09, 0.74]
# 95% CI of case: [-0.18, 0.16]
tab_ci_m_confidence

# MODEL 1.1
# Check if there is an interaction between "group" and "case" on confidence
m_confidence_gc = lmer(confidence ~ 
                         group 
                       * case 
                       + (1|participant) 
                       + (1|question.ref)
                       , data = data)

summary(m_confidence_gc)

# Check if adding group*case term improves the model fit
# Not significant at all: p=1
anova(m_confidence, m_confidence_gc, refit=FALSE)

## MODEL 2
# Random slopes for each participant and each question -- in addition to random intercepts
m_confidence_pq = lmer(confidence ~ 
                         group 
                       + case 
                       + (1+group|participant) 
                       + (1+group|question.ref)
                       , data = data)

summary(m_confidence_pq)
r.squaredGLMM(m_confidence_pq)

# Check if adding random slopes for each participant and each question improves the model fit
# Not significant: p=0.3197
anova(m_confidence, m_confidence_pq, refit=FALSE)

## MODEL 3
# Random slope for each participant, but not for each question -- in addition to random intercepts
# Model failed to converge
m_confidence_p = lmer(confidence ~ 
                        group 
                      + case 
                      + (1+group|participant) 
                      + (1|question.ref)
                      , data = data)

summary(m_confidence_p)
r.squaredGLMM(m_confidence_p)

# Check if adding random slope for each participant (but not each question) improves the model fit
# Not conclusive because model failed to converge
anova(m_confidence, m_confidence_p, refit=FALSE)

## MODEL 4
# Random slope for each question, but not for each participant -- in addition to random intercepts
m_confidence_q = lmer(confidence ~ 
                        group 
                      + case 
                      + (1|participant) 
                      + (1+group|question.ref)
                      , data = data)

summary(m_confidence_q)
r.squaredGLMM(m_confidence_q)

# Check if adding random slope for each question (but not each participant) improves the model fit
# Not significant: p=0.9273
anova(m_confidence, m_confidence_q, refit=FALSE)

# Comparing models based on BIC
# The best model according to BIC is m_confidence
BIC(m_confidence, m_confidence_pq, m_confidence_p, m_confidence_q)

# NOTE: Comment out library(lmerTest) before running stargazer
# Regression table of MDOEL 1 of accuracy, confidence, and score
# stargazer(m_accuracy, m_confidence, m_score, type = "latex", digits = 2)