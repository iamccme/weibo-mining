create table sentiment_words_with_trival(
 `id` int(11) NOT NULL AUTO_INCREMENT,
  `word` varchar(10) NOT NULL,
  `ranking` double(32,12) DEFAULT '0.000000000000',
  assess varchar(255) default '',
  `isSentiment` tinyint(4) DEFAULT '0',
  trival int(11) default 0,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM;