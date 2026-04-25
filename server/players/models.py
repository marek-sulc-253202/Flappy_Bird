from django.db import models

class Player(models.Model):
    name = models.CharField(max_length=50, unique=True)
    score_easy = models.IntegerField(default=0)
    score_normal = models.IntegerField(default=0)
    score_hard = models.IntegerField(default=0)

    def __str__(self):
        return self.name
