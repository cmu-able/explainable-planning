library(lme4)
library(lmerTest)
# library(r2glmm) # R squared
library(MuMIn) # R squared

# setwd("~/Downloads")
data = read.csv("~/Projects/explainable-planning/analysis/data_3qs.csv")
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


m1 = glmer(accuracy ~ 
            group 
            + (1|participant) 
            + (1|question.ref)
          , family = "binomial"
          , data = data)

summary(m1) # experimental group are exp(1.3352) = 3.8 times more likely to answer correctly!
r.squaredGLMM(m1) # goodness of fit of model, without (left) and with (right) random effects
anova(m1) 

table(data$score)

m2 = lmer(score ~ 
             group 
           + (1|participant) 
           + (1|question.ref)
           , data = data)

summary(m2)
r.squaredGLMM(m2)
anova(m2)

