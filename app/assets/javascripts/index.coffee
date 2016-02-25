'use strict'

angular.module('index',['play.routing'])
  .controller('IdentifyCtrl', ($scope, playRoutes) ->
    $scope.text = "The quick brown fox jumps over the lazy dog"
    $scope.$watch('text', _.throttle((text) ->
      playRoutes.controllers.LexicalAnalysisController.identifyGET(text).get().success((data) ->
        $scope.errorStatus = ''
        $scope.guessedLang=data
      ).error((data,status) ->
        if (status==0)
          $scope.errorStatus = 503
          $scope.error = "Service unavailable"
        else
          $scope.errorStatus = status
          $scope.error = data
      )
    ,1000))
  )
  .controller('LemmatizeCtrl', ($scope, playRoutes) ->
    $scope.text = "Albert osti fagotin ja töräytti puhkuvan melodian maakunnanvoudinvirastossa."
    $scope.depth = "1"
    $scope.guess = true
    $scope.$watchCollection('[text,locale,segments,depth]', _.throttle(() ->
      locale = $scope.locale
      if locale=='' then locale=null
      playRoutes.controllers.LexicalAnalysisController.baseformGET($scope.text,locale,$scope.segments, $scope.guess, if ($scope.depth && $scope.depth!="") then $scope.depth else "1").get().success((data) ->
        $scope.errorStatus = ''
        $scope.baseform=data
      ).error((data,status) ->
        if (status==0)
          $scope.errorStatus = 503
          $scope.error = "Service unavailable"
        else
          $scope.errorStatus = status
          $scope.error = data
      )
    ,1000))
  )
  .controller('AnalyzeCtrl', ($scope, playRoutes) ->
    $scope.text = "Albert osti fagotin ja töräytti puhkuvan melodian."
    $scope.locale = "fi"
    $scope.forms = "V N Nom Sg, N Nom Pl, A Pos Nom Pl"
    $scope.guess = true
    $scope.segmentGuessed = true
    $scope.depth = "2"    
    $scope.$watchCollection('[text,locale,forms,segments,depth]', _.throttle(() ->
      locale = $scope.locale
      if locale=='' then locale=null
      playRoutes.controllers.LexicalAnalysisController.analyzeGET($scope.text,locale,$scope.forms.split(/, */),$scope.segments,$scope.guess,$scope.segmentGuessed,if ($scope.depth && $scope.depth!="") then $scope.depth else "2").get().success((data) ->
        $scope.analysis=data
        $scope.errorStatus = ''
        dta = []
        for word,index in data
        	da = null
        	cw = Number.MAX_VALUE
        	for analysis in word.analysis when analysis.globalTags['HEAD']? && analysis.weight<cw
        	    cw=analysis.weight
        	    da = {
        	       dephead:analysis.globalTags['HEAD'][0]
        	       deprel:analysis.globalTags['DEPREL'][0]
        	       pos:analysis.wordParts[analysis.wordParts.length-1].tags['UPOS'][0]
        	       word:word.word
        	       data:word
        	       ref:""+(index+1)
        	    }
        	if da!=null then dta.push(da)
        draw_deptree(dta,'depanalysis',(data) -> if (data.data?.data?) 
          $scope.analysis=[data.data.data]
          $scope.$apply()
        );
      ).error((data,status) ->
        if (status==0)
          $scope.errorStatus = 503
          $scope.error = "Service unavailable"
        else
          $scope.errorStatus = status
          $scope.error = data
      )
    ,1000))
  )
  .controller('InflectionCtrl', ($scope, playRoutes) ->
    $scope.text = "Albert osti fagotin ja töräytti puhkuvan melodian."
    $scope.locale = "fi"
    $scope.baseform=true
    $scope.guess=true
    $scope.forms = "V N Nom Sg, N Nom Pl, A Pos Nom Pl"
    $scope.$watchCollection('[text,locale,segments,baseform,forms]', _.throttle(() ->
      locale = $scope.locale
      if locale=='' then locale=null
      playRoutes.controllers.LexicalAnalysisController.inflectGET($scope.text, $scope.forms.split(/, */),$scope.segments,$scope.baseform,$scope.guess,locale).get().success((data) ->
        $scope.inflection=data
        $scope.errorStatus = ''
      ).error((data,status) ->
        if (status==0)
          $scope.errorStatus = 503
          $scope.error = "Service unavailable"
        else
          $scope.errorStatus = status
          $scope.error = data
      )
    ,1000))
  )
  .controller('HyphenationCtrl', ($scope, playRoutes) ->
    $scope.text = "Albert osti fagotin ja töräytti puhkuvan melodian."
    $scope.$watchCollection('[text,locale]', _.throttle(() ->
      locale = $scope.locale
      if locale=='' then locale=null
      playRoutes.controllers.LexicalAnalysisController.hyphenateGET($scope.text,locale).get().success((data) ->
        $scope.hyphenation=data
        $scope.errorStatus = ''
      ).error((data,status) ->
        if (status==0)
          $scope.errorStatus = 503
          $scope.error = "Service unavailable"
        else
          $scope.errorStatus = status
          $scope.error = data
      )
    ,1000))
  )
