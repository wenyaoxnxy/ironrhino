(function($) {
	var current;
	function find(expr, container) {
		if (!container || expr.indexOf('#') > -1)
			container = document;
		var i = expr.indexOf('@');
		if (i == 0)
			return current;
		else if (i > 0)
			expr = expr.substring(0, i);
		return (expr == 'this') ? current : $(expr, container);
	}
	function val(expr, container, val) {// expr #id #id@attr .class@attr
		// @attr
		if (!container || expr.indexOf('#') > -1)
			container = document;
		if (!expr)
			return;
		if (arguments.length > 2) {
			var i = expr.indexOf('@');
			if (i < 0) {
				var ele = expr == 'this' ? current : $(expr, container);
				ele.each(function() {
							var t = $(this);
							if (t.is(':input')) {
								t.val(val).trigger('change')
										.trigger('validate');
							} else {
								if (val === null && !t.is('td'))
									t
											.html('<i class="glyphicon glyphicon-list"></i>');
								else
									t.text(val);
							}
						});
			} else if (i == 0) {
				current.attr(expr.substring(i + 1), val);
			} else {
				var selector = expr.substring(0, i);
				var ele = selector == 'this' ? current : $(selector, container);
				if (ele.parents('.richtable').length
						&& ele.prop('tagName') == 'TD'
						&& expr.indexOf('data-cellvalue') > -1)
					Richtable.updateValue(ele, val);
				else
					ele.attr(expr.substring(i + 1), val);
			}
		} else {
			var i = expr.indexOf('@');
			if (i < 0) {
				var ele = expr == 'this' ? current : $(expr, container);
				if (ele.is(':input'))
					return ele.val();
				else
					return ele.contents().filter(function() {
								return this.nodeType == 3;
							}).text();
			} else if (i == 0) {
				return current.attr(expr.substring(i + 1));
			} else {
				var selector = expr.substring(0, i);
				var ele = selector == 'this' ? current : $(selector, container);
				return ele.attr(expr.substring(i + 1));
			}
		}
	}
	function removeAction(event) {
		current = $(event.target).closest('.listpick');
		var options = current.data('_options');
		var viewlink = current.find('a.view[rel="richtable"]');
		if (current.is('td') && viewlink.length)
			viewlink.text(null);
		else
			val(options.name, current, null);
		val(options.id, current, null);
		if (options.mapping) {
			for (var k in options.mapping)
				val(k, current, null);
		}
		$(this).remove();
		event.stopPropagation();
		return false;
	}
	$.fn.listpick = function() {
		$(this).each(function() {
			current = $(this);
			var options = {
				id : '.listpick-id',
				name : '.listpick-name',
				idindex : 0,
				nameindex : 1,
				multiple : false
			}
			$.extend(options, (new Function("return "
							+ (current.data('options') || '{}')))());
			current.data('_options', options);
			if (options.name) {
				var nametarget = find(options.name, current);
				var remove = nametarget.children('a.remove');
				if (remove.length) {
					remove.click(removeAction);
				} else {
					var text = val(options.name, current);
					var viewlink = current.find('a.view[rel="richtable"]');
					if (current.is('td') && viewlink.length)
						text = viewlink.text();
					if (text) {
						if (text.indexOf('...') < 0)
							$('<a class="remove" href="#">&times;</a>')
									.appendTo(nametarget).click(removeAction);
					} else {
						val(options.name, current, null);
					}
				}
			}
			var func = function(event) {
				if ($(event.target).is('a.view[rel="richtable"]'))
					return true;
				current = $(event.target).closest('.listpick');
				var options = current.data('_options');
				var winid = current.data('winid');
				if (winid) {
					$('#' + winid).remove();
				} else {
					var winindex = $(document).data('winindex') || 0;
					winindex++;
					$(document).data('winindex', winindex);
					var winid = '_window_' + winindex;
					current.data('winid', winid);
				}
				var win = $('<div id="' + winid + '" title="'
						+ MessageBundle.get('select')
						+ '" class="window-listpick"></div>')
						.appendTo(document.body).dialog({
									width : options.width || 800,
									minHeight : options.minHeight || 500,
									close : function() {
										win.html('').dialog('destroy').remove();
									}
								});
				win.data('windowoptions', options);
				win.data('selected', val(options.id, current) || '');
				win.closest('.ui-dialog').css('z-index', 2000);
				if (win.html() && typeof $.fn.mask != 'undefined')
					win.mask();
				else
					win.html('<div style="text-align:center;">'
							+ MessageBundle.get('ajax.loading') + '</div>');
				var target = win.get(0);
				target.onsuccess = function() {
					$(target).data('listpick', current);
					if (typeof $.fn.mask != 'undefined')
						win.unmask();
					Dialog.adapt(win);
					if (!options.multiple) {
						$(target).on('click', 'tbody input[type=radio]',
								function() {
									var cell = $($(this).closest('tr')[0].cells[options.idindex]);
									var id = options.idindex == 0 ? $(this)
											.val() : cell.data('cellvalue')
											|| cell.text();
									cell = $($(this).closest('tr')[0].cells[options.nameindex]);
									var name = cell.data('cellvalue')
											|| cell.text();
									if (options.name) {
										var nametarget = find(options.name,
												$(target).data('listpick'));
										var viewlink = nametarget
												.find('a.view[rel="richtable"]');
										if (nametarget.is('td')
												&& viewlink.length) {
											var href = viewlink.attr('href');
											viewlink
													.attr(
															'href',
															href
																	.substring(
																			0,
																			href
																					.lastIndexOf('/')
																					+ 1)
																	+ id);
											viewlink.text(name);
										} else
											val(options.name, $(target)
															.data('listpick'),
													name);
										nametarget.each(function() {
											var t = $(this);
											if (!t.is(':input')
													&& !t.find('a.remove').length)
												$('<a class="remove" href="#">&times;</a>')
														.appendTo(t)
														.click(removeAction);
										});
									}
									if (options.id) {
										val(options.id, $(target)
														.data('listpick'), id);
										var idtarget = find(options.id,
												$(target).data('listpick'));
									}
									if (options.mapping) {
										for (var k in options.mapping) {
											cell = $($(this).closest('tr')[0].cells[options.mapping[k]]);
											val(k, $(target).data('listpick'),
													cell.data('cellvalue')
															|| cell.text());
										}
									}
									win.dialog('close');
									return false;
								});

					} else {
						$(target).on('click', 'button.pick', function() {
							var idSeparator = ',';
							var nameSeparator = ', ';
							var checked = {};
							var uncheckedIds = [];
							var uncheckedNames = [];
							$('table.richtable tbody input[type="checkbox"]',
									target).each(function() {
								var cell = $($(this).closest('tr')[0].cells[options.idindex]);
								var id = options.idindex == 0
										? $(this).val()
										: cell.data('cellvalue') || cell.text();
								cell = $($(this).closest('tr')[0].cells[options.nameindex]);
								var name = cell.data('cellvalue')
										|| cell.text();
								if (this.checked) {
									checked[id] = name
								} else {
									uncheckedIds.push(id);
									uncheckedNames.push(name);
								};
							});
							var selectedIds = [];
							if (options.id) {
								var v = val(options.id, $(target)
												.data('listpick'));
								if (v)
									selectedIds = v.split(idSeparator);
							}
							var selectedNames = [];
							if (options.name) {
								var v = val(options.name, $(target)
												.data('listpick'));
								if (v)
									selectedNames = v.split(nameSeparator);
								if (options.id) {
									if (selectedNames.length)
										$.each(uncheckedIds, function(i, v) {
													var index = selectedIds
															.indexOf(v);
													if (index > -1) {
														selectedIds.splice(
																index, 1);
														selectedNames.splice(
																index, 1);
													}
												});
									for (var key in checked) {
										if (selectedIds.indexOf(key) < 0) {
											selectedIds.push(key);
											selectedNames.push(checked[key]);
										}
									}
								} else {
									if (selectedNames.length)
										$.each(uncheckedNames, function(i, v) {
													var index = uncheckedNames
															.indexOf(v);
													if (index > -1)
														selectedNames.splice(
																index, 1);
												});
									for (var key in checked)
										if (selectedNames.indexOf(checked[key]) < 0)
											selectedNames.push(checked[key]);
								}
							}
							if (options.id && !options.name) {
								if (selectedIds.length)
									$.each(uncheckedIds, function(i, v) {
												var index = selectedIds
														.indexOf(v);
												if (index > -1)
													selectedIds
															.splice(index, 1);
											});
								for (var key in checked)
									if (selectedIds.indexOf(key) < 0)
										selectedIds.push(key);
							}
							if (options.id) {
								find(options.id, $(target).data('listpick'))
										.each(function() {
											var t = $(this);
											val(options.id, $(target)
															.data('listpick'),
													selectedIds
															.join(idSeparator));
										});

							}
							if (options.name) {
								find(options.name, $(target).data('listpick'))
										.each(function() {
											var t = $(this);
											if (selectedNames.length) {
												val(
														options.name,
														$(target)
																.data('listpick'),
														selectedNames
																.join(idSeparator));
												if (!t.is(':input')
														&& !t.find('.remove').length)
													$('<a class="remove" href="#">&times;</a>')
															.appendTo(t)
															.click(removeAction);
											} else {
												if (!t.is(':input')) {
													t.find('.remove').click();
												} else {
													t.val('');
												}
											}
										});
							}

							win.dialog('close');
							return false;
						});
					}
				};
				var url = options.url;
				if (url.indexOf('multiple') < 0 && options.multiple)
					url += (url.indexOf('?') > 0 ? '&' : '?') + 'multiple=true'
				ajax({
							url : url,
							cache : false,
							target : target,
							replacement : winid + ':content',
							quiet : true
						});

			};
			var handle = current.find('.listpick-handle');
			if (!handle.length)
				handle = current;
			handle.css('cursor', 'pointer').click(func).keydown(
					function(event) {
						if (event.keyCode == 13) {
							func(event);
							return false;
						}
					});
		});
		return this;
	};

})(jQuery);

Observation.listpick = function(container) {
	$$('.listpick', container).listpick();
	$$('form.pick.richtable').each(function() {
				var t = $(this);
				var selected = t.closest('.window-listpick').data('selected');
				if (selected) {
					var arr = selected.split(',');
					$('input[type="checkbox"]', t).each(function() {
								if ($.inArray(this.value, arr) > -1)
									$(this).click();
							});
					$('input[type="radio"]', t).each(function() {
								if (this.value == selected)
									this.checked = true;
							});
				}
			});
};